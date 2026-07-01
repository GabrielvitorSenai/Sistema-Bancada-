package com.tecdes.sistema_bancada.controller;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.tecdes.sistema_bancada.dto.BlocoDTO;
import com.tecdes.sistema_bancada.dto.LaminaDTO;
import com.tecdes.sistema_bancada.dto.PedidoDTO;
import com.tecdes.sistema_bancada.model.Estoque;
import com.tecdes.sistema_bancada.model.Expedicao;
import com.tecdes.sistema_bancada.model.Pedido;
import com.tecdes.sistema_bancada.repository.EstoqueRepository;
import com.tecdes.sistema_bancada.repository.ExpedicaoRepository;
import com.tecdes.sistema_bancada.repository.PedidoRepository;
import com.tecdes.sistema_bancada.service.PlcConnector;
import com.tecdes.sistema_bancada.service.SmartService;
import com.tecdes.sistema_bancada.service.SmartService.PlcConnectionManager;

@RestController
public class SmartController {

    private final Map<Long, Integer> posicoesExpedicaoPedidos = new ConcurrentHashMap<>();

    @Autowired
    private SmartService smartService;

    @Autowired
    private EstoqueRepository estoqueRepository;

    @Autowired
    private ExpedicaoRepository expedicaoRepository;

    @Autowired
    private PedidoRepository pedidoRepository;

    @PostMapping("/iniciar-pedido")
    public ResponseEntity<String> iniciarPedido(@RequestBody PedidoDTO pedidoDTO) {
        Long idPedido = pedidoDTO.getId();
        String tipo = pedidoDTO.getTipo();
        int tampa = pedidoDTO.getTampa();
        String ipClp = pedidoDTO.getIpClp();
        List<BlocoDTO> pedido = pedidoDTO.getBlocos();

        // Posição de guardar na expedição (1..12). Se vier 0/inválida, usa a 1ª livre.
        int posExpedicao = pedidoDTO.getPosicaoExpedicao();
        if (posExpedicao < 1 || posExpedicao > 12) {
            posExpedicao = smartService.buscarPrimeiraPosicaoLivreExp();
        }
        if (posExpedicao == -1) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Erro: não há posição livre na expedição para guardar o pedido.");
        }

        System.out.println("Iniciando pedido ID: " + idPedido);
        System.out.println("Pedido recebido para IP do CLP: " + ipClp);
        System.out.println("Pedido tipo: " + tipo);
        System.out.println("Posição de expedição (guardar ao finalizar): " + posExpedicao);
        System.out.println("Cor da tampa: " + (tampa == 1 ? "Preto" : tampa == 2 ? "Vermelho" : "Azul"));

        for (BlocoDTO bloco : pedido) {
            System.out.println("Andar: " + bloco.getAndar() + ", Cor do Bloco: " + bloco.getCorBloco());
            int i = 1;
            for (LaminaDTO lamina : bloco.getLaminas()) {
                System.out.println("  Lâmina-" + i + ": Cor = " + lamina.getCor() + ", Padrão = " + lamina.getPadrao());
                i++;
            }
        }

        try {
            byte[] bytePedidoArray = montarPedidoParaCLP(pedido, idPedido, posExpedicao);

            System.out.print("Bytes do pedido em hexadecimal: ");
            for (byte b : bytePedidoArray) {
                System.out.printf("%02X ", b);
            }
            System.out.println();

            // 1) Envia o pedido ao CLP de Estoque.
            boolean envioClpOk = smartService.enviarBlocoBytesAoClp(ipClp, 9, 2, bytePedidoArray, bytePedidoArray.length);
            if (!envioClpOk) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Erro: falha ao enviar bloco de bytes ao CLP.");
            }

            // 2) Seleciona a tampa.
            try {
                RestTemplate apiSeletorTampa = new RestTemplate();
                String url = "http://10.74.241.245/api/move_pos?pos=" + tampa;

                // O seletor de tampas aceita POST nesta rota.
                ResponseEntity<Map> response = apiSeletorTampa.postForEntity(url, null, Map.class);
                Map<String, Object> body = response.getBody();

                if (body == null || !body.containsKey("status")) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body("Erro: seletor de tampas não retornou resposta válida.");
                }

                String status = body.get("status").toString();
                System.out.println("Resposta do seletor de tampas: " + status);

                if (!status.contains("Ok")) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body("Erro: seletor de tampas não confirmou a posição. Resposta: " + status);
                }
            } catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Erro ao comunicar com o seletor de tampas: " + e.getMessage());
            }

            // 3) Guarda a posição escolhida apenas em memória para usar na finalização.
            // Não grava a Expedição no banco/CLP aqui, porque o produto ainda não terminou.
            if (idPedido != null) {
                posicoesExpedicaoPedidos.put(idPedido, posExpedicao);
            }

            // Registra qual pedido está em curso: sinais do CLP que não se referirem a
            // este número de OP são tratados como resíduo do pedido anterior e ignorados.
            SmartService.pedidoAtualId = idPedido != null ? idPedido.intValue() : 0;

            // 4) Inicia o pedido sem recalcular a posição de expedição.
            boolean inicioOk = iniciarExecucaoPedidoNaPosicao(ipClp, posExpedicao);
            if (!inicioOk) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Erro: não foi possível acionar a flag de início no CLP.");
            }

            return ResponseEntity.ok("Pedido enviado e iniciado no CLP com sucesso.");

        } catch (IllegalStateException e) {
            System.out.println("Pedido bloqueado por estoque: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao processar pedido: " + e.getMessage());
        }
    }

    @PostMapping("/finalizar-pedido-producao")
    public ResponseEntity<String> finalizarPedidoProducao(@RequestBody PedidoDTO pedidoDTO) {
        Long idPedido = pedidoDTO.getId();
        if (idPedido == null) {
            return ResponseEntity.badRequest().body("ID do pedido não informado para finalização.");
        }

        int posExpedicao = pedidoDTO.getPosicaoExpedicao();
        if (posExpedicao < 1 || posExpedicao > 12) {
            posExpedicao = posicoesExpedicaoPedidos.getOrDefault(idPedido, SmartService.posicaoExpedicaoSolicitada);
        }

        if (posExpedicao < 1 || posExpedicao > 12) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Posição de expedição inválida para finalizar o pedido " + idPedido + ".");
        }

        String ipClpExpedicao = pedidoDTO.getIpClp();
        if (ipClpExpedicao == null || ipClpExpedicao.isBlank()) {
            return ResponseEntity.badRequest().body("IP do CLP de Expedição não informado para finalização.");
        }

        // Mantém o estado compartilhado apontando para a mesma posição usada aqui,
        // para que nenhum outro fluxo trabalhe com uma posição divergente.
        SmartService.posicaoExpedicaoSolicitada = posExpedicao;

        try {
            Optional<Pedido> pedidoOptional = pedidoRepository.findById(idPedido);
            if (pedidoOptional.isPresent()) {
                Pedido pedido = pedidoOptional.get();
                pedido.setStatusOrderProduction("concluido");
                pedidoRepository.save(pedido);
            }

            // Agora sim grava a expedição no banco, porque o pedido terminou.
            Expedicao exp = expedicaoRepository.findByPosicaoExpedicao(posExpedicao)
                    .orElseGet(Expedicao::new);
            exp.setPosicaoExpedicao(posExpedicao);
            exp.setOrderNumber(idPedido.intValue());
            expedicaoRepository.save(exp);

            // Agora sim escreve SOMENTE a posição finalizada no CLP.
            // Não enviamos mais o bloco inteiro de 24 bytes, para não sobrescrever outras posições já existentes.
            boolean expedicaoEnviada = enviarPosicaoExpedicaoParaClp(ipClpExpedicao, posExpedicao, idPedido.intValue());
            if (!expedicaoEnviada) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Pedido finalizado no banco, mas falhou ao gravar a posição " + posExpedicao
                                + " no CLP " + ipClpExpedicao + ".");
            }

            posicoesExpedicaoPedidos.remove(idPedido);
            SmartService.pedidoEmCurso = false;
            SmartService.statusProducao = 1;

            return ResponseEntity.ok("Pedido " + idPedido + " finalizado e posição " + posExpedicao
                    + " gravada no CLP de Expedição.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao finalizar pedido: " + e.getMessage());
        }
    }

    private boolean iniciarExecucaoPedidoNaPosicao(String ipClp, int posExpedicao) {
        // Deixa a posição fixa para o fluxo da Expedição responder quando pedir posição.
        SmartService.posicaoExpedicaoSolicitada = posExpedicao;
        SmartService.pedidoEmCurso = true;
        SmartService.statusProducao = 0;

        if (SmartService.readOnly) {
            return true;
        }

        PlcConnector plcConnector = PlcConnectionManager.getConexao(ipClp);
        if (plcConnector == null) {
            return false;
        }

        try {
            // Limpa flags antes de acionar o início.
            plcConnector.writeBit(9, 0, 0, false);
            plcConnector.writeBit(9, 64, 0, false);
            plcConnector.writeBit(9, 64, 1, false);
            plcConnector.writeBit(9, 62, 0, false);

            System.out.println("INICIAR PEDIDO - posição expedição fixa: " + posExpedicao);
            plcConnector.writeBit(9, 62, 0, true);
            return true;
        } catch (Exception e) {
            System.out.println("ERRO: falha ao iniciar execução do pedido no CLP de Estoque.");
            e.printStackTrace();
            return false;
        }
    }

    private boolean enviarPosicaoExpedicaoParaClp(String ipClpExpedicao, int posExpedicao, int orderNumber) {
        if (SmartService.readOnly) {
            return true;
        }

        if (posExpedicao < 1 || posExpedicao > 12) {
            System.out.println("Posição de expedição inválida para escrita pontual: " + posExpedicao);
            return false;
        }

        PlcConnector plcConnector = PlcConnectionManager.getConexao(ipClpExpedicao);
        if (plcConnector == null) {
            return false;
        }

        int offset = 6 + ((posExpedicao - 1) * 2);
        try {
            System.out.println("Gravando apenas a posição " + posExpedicao + " da Expedição no CLP. Offset DB9:"
                    + offset + " OP:" + orderNumber);
            plcConnector.writeInt(9, offset, orderNumber);
            return true;
        } catch (Exception e) {
            System.out.println("ERRO: falha ao gravar posição pontual da Expedição no CLP.");
            e.printStackTrace();
            return false;
        }
    }

    private boolean enviarExpedicaoAtualParaClp(String ipClpExpedicao) {
        List<Expedicao> listaExpedicao = expedicaoRepository.findAll();
        byte[] byteBlocosArray = montarBytesExpedicao(listaExpedicao);

        System.out.print("Bytes enviados ao CLP Expedição: ");
        for (byte b : byteBlocosArray) {
            System.out.printf("%02X ", b);
        }
        System.out.println();

        return smartService.enviarBlocoBytesAoClp(ipClpExpedicao, 9, 6, byteBlocosArray, byteBlocosArray.length);
    }

    private byte[] montarBytesExpedicao(List<Expedicao> listaExpedicao) {
        byte[] byteBlocosArray = new byte[24];

        for (Expedicao e : listaExpedicao) {
            int pos = e.getPosicaoExpedicao();
            int valor = e.getOrderNumber();

            if (pos >= 1 && pos <= 12) {
                int index = (pos - 1) * 2;
                byteBlocosArray[index] = (byte) (valor >> 8);
                byteBlocosArray[index + 1] = (byte) (valor & 0xFF);
            }
        }

        return byteBlocosArray;
    }

    private String nomeCor(int cor) {
        switch (cor) {
            case 1:
                return "Preto";
            case 2:
                return "Vermelho";
            case 3:
                return "Azul";
            default:
                return "Cor " + cor;
        }
    }

    @GetMapping("/estoque/disponibilidade")
    public ResponseEntity<Map<Integer, Integer>> disponibilidadeEstoque() {
        Map<Integer, Integer> contagem = new java.util.HashMap<>();
        contagem.put(1, 0);
        contagem.put(2, 0);
        contagem.put(3, 0);

        for (Estoque e : estoqueRepository.findAll()) {
            int cor = e.getCor();
            if (cor >= 1 && cor <= 3) {
                contagem.put(cor, contagem.get(cor) + 1);
            }
        }

        return ResponseEntity.ok(contagem);
    }

    @PostMapping("/estoque/salvar")
    public ResponseEntity<String> salvarEstoque(@RequestBody Map<String, Integer> dados) {
        try {
            dados.forEach((posStr, valor) -> {
                try {
                    int pos = Integer.parseInt(posStr.split(":")[1]);

                    if (pos >= 1 && pos <= 28) {
                        Estoque estoque = estoqueRepository.findByPosicaoEstoque(pos)
                                .orElseGet(() -> {
                                    Estoque novo = new Estoque();
                                    novo.setPosicaoEstoque(pos);
                                    return novo;
                                });

                        estoque.setCor(valor);
                        estoqueRepository.save(estoque);
                    }
                } catch (Exception e) {
                    System.err.println("Erro ao processar posição: " + posStr + " - " + e.getMessage());
                }
            });

            return ResponseEntity.ok("Estoque salvo com sucesso.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao salvar estoque: " + e.getMessage());
        }
    }

    @PostMapping("/expedicao/salvar")
    public ResponseEntity<String> salvarExpedicao(@RequestBody Map<String, Integer> dados) {
        System.out.println("Atualizando tabela Expedição!!");

        try {
            dados.forEach((posStr, valor) -> {
                try {
                    int pos = Integer.parseInt(posStr.split(":")[1]);

                    if (pos >= 1 && pos <= 12) {
                        if (valor == 0) {
                            expedicaoRepository.findByPosicaoExpedicao(pos).ifPresent(expedicaoRepository::delete);
                            System.out.println("Removida posição " + pos + " da tabela Expedição.");
                        } else {
                            Expedicao exp = expedicaoRepository
                                    .findByPosicaoExpedicao(pos)
                                    .orElseGet(Expedicao::new);

                            exp.setPosicaoExpedicao(pos);
                            exp.setOrderNumber(valor);
                            expedicaoRepository.save(exp);
                            System.out.println("Atualizada posição " + pos + " com valor " + valor);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Erro ao processar posição: " + posStr + " - " + e.getMessage());
                }
            });

            return ResponseEntity.ok("Tabela Expedição atualizada com sucesso.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao atualizar tabela Expedição: " + e.getMessage());
        }
    }

    @PostMapping("/clp/enviar-estoque")
    public ResponseEntity<String> enviarParaClp(@RequestBody Map<String, String> payload) {
        try {
            String ipClpEstoque = payload.get("ipClp");

            if (ipClpEstoque == null || ipClpEstoque.isEmpty()) {
                return ResponseEntity.badRequest().body("Endereço IP do CLP de Estoque não fornecido.");
            }

            List<Estoque> listaEstoque = estoqueRepository.findAll();
            byte[] byteBlocosArray = new byte[28];

            for (Estoque e : listaEstoque) {
                int pos = e.getPosicaoEstoque();
                if (pos >= 1 && pos <= 28) {
                    byteBlocosArray[pos - 1] = (byte) e.getCor();
                }
            }

            System.out.print("Bytes enviados ao CLP Estoque: ");
            for (byte b : byteBlocosArray) {
                System.out.printf("%02X ", b);
            }
            System.out.println();

            boolean ok = smartService.enviarBlocoBytesAoClp(ipClpEstoque, 9, 68, byteBlocosArray, byteBlocosArray.length);
            if (!ok) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Erro: falha ao enviar dados ao CLP de Estoque.");
            }

            return ResponseEntity.ok("Bloco de bytes enviado com sucesso para o CLP de Estoque.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao enviar dados ao CLP: " + e.getMessage());
        }
    }

    @PostMapping("/clp/enviar-expedicao")
    public ResponseEntity<String> enviarParaClpExpedicao(@RequestBody Map<String, String> payload) {
        try {
            String ipClpExpedicao = payload.get("ipClp");

            if (ipClpExpedicao == null || ipClpExpedicao.isEmpty()) {
                return ResponseEntity.badRequest().body("Endereço IP do CLP de Expedição não fornecido.");
            }

            boolean ok = enviarExpedicaoAtualParaClp(ipClpExpedicao);
            if (!ok) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Erro: falha ao enviar dados ao CLP de Expedição.");
            }

            return ResponseEntity.ok("Bloco de inteiros enviado com sucesso para o CLP de Expedição.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao enviar dados ao CLP de Expedição: " + e.getMessage());
        }
    }

    @GetMapping("/estoque/primeira-posicao/{cor}")
    public ResponseEntity<Integer> getPrimeiraPosicaoPorCor(@PathVariable int cor) {
        Set<Integer> posicoesUsadas = new HashSet<>();
        int posicao = smartService.buscarPrimeiraPosicaoPorCor(cor, posicoesUsadas);

        if (posicao != -1) {
            return ResponseEntity.ok(posicao);
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(-1);
    }

    @GetMapping("/expedicao/primeira-livre")
    public ResponseEntity<Integer> buscarLivre() {
        int posicaoLivre = smartService.buscarPrimeiraPosicaoLivreExp();
        return ResponseEntity.ok(posicaoLivre);
    }

    private byte[] montarPedidoParaCLP(List<BlocoDTO> pedido, Long idPedido, int posicaoExpedicao) {
        int[] dados = new int[30];
        Set<Integer> posicoesUsadas = new HashSet<>();
        int andares = pedido.size();

        for (BlocoDTO bloco : pedido) {
            int indexBase = (bloco.getAndar() - 1) * 9;

            if (indexBase + 8 >= dados.length) {
                System.out.println("Ignorando andar fora do esperado: " + bloco.getAndar());
                continue;
            }

            int corBloco = bloco.getCorBloco();
            int posicaoEstoque = smartService.buscarPrimeiraPosicaoPorCor(corBloco, posicoesUsadas);

            if (posicaoEstoque == -1) {
                throw new IllegalStateException(
                        "Sem bloco da cor " + nomeCor(corBloco) + " disponível no estoque (andar "
                                + bloco.getAndar() + ").");
            }

            posicoesUsadas.add(posicaoEstoque);

            dados[indexBase] = corBloco;
            dados[indexBase + 1] = posicaoEstoque;

            List<LaminaDTO> laminas = bloco.getLaminas();
            for (int i = 0; i < Math.min(3, laminas.size()); i++) {
                dados[indexBase + 2 + i] = laminas.get(i).getCor();
                dados[indexBase + 5 + i] = laminas.get(i).getPadrao();
            }

            dados[indexBase + 8] = 0;
        }

        dados[27] = idPedido != null ? idPedido.intValue() : 0;
        dados[28] = andares;
        dados[29] = posicaoExpedicao;

        System.out.println("// InfoPedido");
        for (int andar = 1; andar <= 3; andar++) {
            int base = (andar - 1) * 9;
            System.out.println("cor_Andar_" + andar + " = " + dados[base] + ";");
            System.out.println("posicao_Estoque_Andar_" + andar + ".......: " + dados[base + 1]);
            System.out.println("cor_Lamina_1_Andar_" + andar + "..........: " + dados[base + 2]);
            System.out.println("cor_Lamina_2_Andar_" + andar + "..........: " + dados[base + 3]);
            System.out.println("cor_Lamina_3_Andar_" + andar + "..........: " + dados[base + 4]);
            System.out.println("padrao_Lamina_1_Andar_" + andar + ".......: " + dados[base + 5]);
            System.out.println("padrao_Lamina_2_Andar_" + andar + ".......: " + dados[base + 6]);
            System.out.println("padrao_Lamina_3_Andar_" + andar + ".......: " + dados[base + 7]);
            System.out.println("processamento_Andar_" + andar + ".........: " + dados[base + 8]);
            System.out.println();
        }

        System.out.println("numeroPedidoEst...............: " + idPedido);
        System.out.println("andares.......................: " + andares);
        System.out.println("posicaoExpedicaoEst...........: " + posicaoExpedicao);

        ByteBuffer buffer = ByteBuffer.allocate(60).order(ByteOrder.BIG_ENDIAN);
        for (int valor : dados) {
            buffer.putShort((short) valor);
        }

        return buffer.array();
    }
}
