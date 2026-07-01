package com.tecdes.sistema_bancada.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.tecdes.sistema_bancada.model.Expedicao;
import com.tecdes.sistema_bancada.repository.ExpedicaoRepository;
import com.tecdes.sistema_bancada.service.PlcConnector;
import com.tecdes.sistema_bancada.service.SmartService;
import com.tecdes.sistema_bancada.service.SmartService.PlcConnectionManager;

/**
 * Controller auxiliar para preparar e corrigir o Magazine de Expedição.
 *
 * Ele concentra as rotas extras criadas para evitar que o pedido novo seja
 * gravado em posição errada ou duplicada no CLP de Expedição.
 */
@RestController
public class ExpedicaoCorrecaoController {

    /** Repositório usado para ler e atualizar posições da expedição no banco. */
    @Autowired
    private ExpedicaoRepository expedicaoRepository;

    /**
     * Prepara a posição correta da expedição antes de iniciar o pedido.
     *
     * A rotina antiga do CLP usa uma posição interna para saber onde guardar o pedido.
     * Por isso este endpoint escreve a posição selecionada no DB9 offset 4 do CLP.
     */
    @PostMapping("/expedicao/preparar-posicao")
    public ResponseEntity<String> prepararPosicao(@RequestBody Map<String, Object> payload) {
        try {
            String ipClp = asString(payload.get("ipClp"));
            int posicao = asInt(payload.get("posicao"));

            if (posicao < 1 || posicao > 12) {
                return ResponseEntity.badRequest().body("Posição de expedição inválida.");
            }

            if (ipClp == null || ipClp.isBlank()) {
                return ResponseEntity.badRequest().body("IP do CLP de Expedição não informado.");
            }

            boolean ok = prepararPosicaoNoClp(ipClp, posicao);
            if (!ok) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Falha ao preparar posição no CLP de Expedição.");
            }

            // Guarda a mesma posição em memória para o restante do fluxo usar a referência correta.
            SmartService.posicaoExpedicaoSolicitada = posicao;

            return ResponseEntity.ok("Posição " + posicao + " preparada no CLP de Expedição.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao preparar posição da expedição: " + e.getMessage());
        }
    }

    /**
     * Corrige duplicidade depois que um pedido finaliza.
     *
     * A regra é não-destrutiva:
     * 1. a posição correta recebe o pedido novo;
     * 2. qualquer outra posição com o mesmo pedido é considerada duplicada e limpa;
     * 3. posições que possuem outros pedidos não são alteradas.
     */
    @PostMapping("/expedicao/corrigir-duplicidade")
    public ResponseEntity<String> corrigirDuplicidade(@RequestBody Map<String, Object> payload) {
        try {
            String ipClp = asString(payload.get("ipClp"));
            int pedidoId = asInt(payload.get("pedidoId"));
            int posicaoCorreta = asInt(payload.get("posicaoCorreta"));

            if (pedidoId <= 0) {
                return ResponseEntity.badRequest().body("Pedido inválido para correção da expedição.");
            }

            if (posicaoCorreta < 1 || posicaoCorreta > 12) {
                return ResponseEntity.badRequest().body("Posição correta inválida para correção da expedição.");
            }

            if (ipClp == null || ipClp.isBlank()) {
                return ResponseEntity.badRequest().body("IP do CLP de Expedição não informado.");
            }

            // Percorre as 12 posições da expedição para garantir que a OP fique apenas onde deve ficar.
            for (int posicao = 1; posicao <= 12; posicao++) {
                if (posicao == posicaoCorreta) {
                    salvarPosicaoNoBanco(posicao, pedidoId);
                    boolean escrito = escreverPosicaoNoClp(ipClp, posicao, pedidoId);
                    if (!escrito) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body("Falha ao gravar posição " + posicao + " no CLP de Expedição.");
                    }
                    continue;
                }

                // Remove duplicidade no banco somente se aquela posição contém o mesmo pedido.
                boolean duplicataNoBanco = expedicaoRepository.findByPosicaoExpedicao(posicao)
                        .map(exp -> exp.getOrderNumber() == pedidoId)
                        .orElse(false);
                if (duplicataNoBanco) {
                    salvarPosicaoNoBanco(posicao, 0);
                    System.out.println("Duplicata do pedido " + pedidoId
                            + " removida do banco na posição " + posicao + ".");
                }

                // Remove duplicidade no CLP somente se a posição contém o mesmo pedido.
                // Em vez de zerar, restaura o valor salvo no banco para aquela posição,
                // preservando OPs de outros pedidos que o CLP tenha sobrescrito.
                Integer valorNoClp = lerPosicaoNoClp(ipClp, posicao);
                if (valorNoClp != null && valorNoClp == pedidoId) {
                    int valorBanco = expedicaoRepository.findByPosicaoExpedicao(posicao)
                            .map(Expedicao::getOrderNumber)
                            .orElse(0);
                    if (valorBanco == pedidoId) {
                        valorBanco = 0; // nunca mantém a mesma OP em duas posições
                    }

                    boolean restaurado = escreverPosicaoNoClp(ipClp, posicao, valorBanco);
                    if (restaurado) {
                        System.out.println("Duplicata do pedido " + pedidoId
                                + " removida do CLP na posição " + posicao
                                + " (valor restaurado: " + valorBanco + ").");
                    }
                }
            }

            return ResponseEntity.ok("Reconciliação da expedição concluída: pedido " + pedidoId
                    + " mantido apenas na posição " + posicaoCorreta + ".");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao corrigir duplicidade da expedição: " + e.getMessage());
        }
    }

    /**
     * Escreve no CLP qual posição deve ser usada para guardar o pedido.
     */
    private boolean prepararPosicaoNoClp(String ipClp, int posicao) {
        if (SmartService.readOnly) {
            return true;
        }

        PlcConnector plcConnector = PlcConnectionManager.getConexao(ipClp);
        if (plcConnector == null) {
            return false;
        }

        try {
            System.out.println("Preparando posição da Expedição no CLP. DB9:4 = " + posicao);

            // Limpa flags antigas da expedição antes de escrever a nova posição.
            plcConnector.writeBit(9, 2, 1, false);
            plcConnector.writeBit(9, 2, 0, false);

            // DB9 offset 4 é a posição onde o CLP deve guardar o pedido finalizado.
            plcConnector.writeInt(9, 4, posicao);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /** Salva uma posição no banco; valor zero remove a posição da tabela. */
    private void salvarPosicaoNoBanco(int posicao, int valor) {
        if (valor <= 0) {
            expedicaoRepository.findByPosicaoExpedicao(posicao).ifPresent(expedicaoRepository::delete);
            return;
        }

        Expedicao exp = expedicaoRepository.findByPosicaoExpedicao(posicao)
                .orElseGet(Expedicao::new);
        exp.setPosicaoExpedicao(posicao);
        exp.setOrderNumber(valor);
        expedicaoRepository.save(exp);
    }

    /** Lê uma posição da expedição diretamente da memória do CLP. */
    private Integer lerPosicaoNoClp(String ipClp, int posicao) {
        if (SmartService.readOnly) {
            return null;
        }

        PlcConnector plcConnector = PlcConnectionManager.getConexao(ipClp);
        if (plcConnector == null) {
            return null;
        }

        int offset = 6 + ((posicao - 1) * 2);
        try {
            return plcConnector.readInt(9, offset);
        } catch (Exception e) {
            System.out.println("Aviso: não foi possível ler a posição " + posicao
                    + " do CLP de Expedição para verificar duplicata: " + e.getMessage());
            return null;
        }
    }

    /**
     * Escreve uma única posição da expedição no CLP.
     *
     * Offset = 6 + ((posição - 1) * 2), porque cada posição ocupa uma word de 2 bytes.
     */
    private boolean escreverPosicaoNoClp(String ipClp, int posicao, int valor) {
        if (SmartService.readOnly) {
            return true;
        }

        PlcConnector plcConnector = PlcConnectionManager.getConexao(ipClp);
        if (plcConnector == null) {
            return false;
        }

        int offset = 6 + ((posicao - 1) * 2);
        try {
            System.out.println("Reconciliando expedição - posição " + posicao + " offset DB9:"
                    + offset + " valor:" + valor);
            plcConnector.writeInt(9, offset, valor);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /** Converte valores recebidos pelo JSON para inteiro com segurança. */
    private int asInt(Object valor) {
        if (valor == null) {
            return 0;
        }

        if (valor instanceof Number n) {
            return n.intValue();
        }

        try {
            return Integer.parseInt(valor.toString());
        } catch (Exception e) {
            return 0;
        }
    }

    /** Converte valores recebidos pelo JSON para texto com segurança. */
    private String asString(Object valor) {
        return valor == null ? null : valor.toString();
    }
}
