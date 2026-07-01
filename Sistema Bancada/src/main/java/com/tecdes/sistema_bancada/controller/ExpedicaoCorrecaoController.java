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

@RestController
public class ExpedicaoCorrecaoController {

    @Autowired
    private ExpedicaoRepository expedicaoRepository;

    @PostMapping("/expedicao/corrigir-duplicidade")
    public ResponseEntity<String> corrigirDuplicidade(@RequestBody Map<String, Object> payload) {
        try {
            String ipClp = asString(payload.get("ipClp"));
            int pedidoId = asInt(payload.get("pedidoId"));
            int posicaoCorreta = asInt(payload.get("posicaoCorreta"));
            // O campo "snapshot" ainda pode vir no payload (compatibilidade com o front),
            // mas não é mais utilizado: a reconciliação agora é não-destrutiva e não
            // sobrescreve posições de outros pedidos.

            if (pedidoId <= 0) {
                return ResponseEntity.badRequest().body("Pedido inválido para correção da expedição.");
            }

            if (posicaoCorreta < 1 || posicaoCorreta > 12) {
                return ResponseEntity.badRequest().body("Posição correta inválida para correção da expedição.");
            }

            if (ipClp == null || ipClp.isBlank()) {
                return ResponseEntity.badRequest().body("IP do CLP de Expedição não informado.");
            }

            // CORREÇÃO: reconciliação NÃO-DESTRUTIVA.
            //
            // Antes: todas as 12 posições eram "restauradas" a partir do snapshot. Quando o
            // snapshot chegava vazio (sessionStorage limpo, outra aba, corrida com o botão
            // "Pedido concluido"), valorSnapshot() devolvia 0 para todas as posições e o
            // sistema APAGAVA os pedidos anteriores do banco e do CLP.
            //
            // Agora: só mexemos no que diz respeito a ESTE pedido.
            //   1. A posição correta recebe o pedidoId (garantia de gravação).
            //   2. Qualquer OUTRA posição que contenha o MESMO pedidoId é uma duplicata
            //      e é limpa (banco + CLP).
            //   3. Todas as demais posições ficam INTOCADAS - o snapshot não é mais usado
            //      para sobrescrever nada.
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

                // Remove duplicata no banco (apenas se a posição contém ESTE pedido).
                boolean duplicataNoBanco = expedicaoRepository.findByPosicaoExpedicao(posicao)
                        .map(exp -> exp.getOrderNumber() == pedidoId)
                        .orElse(false);
                if (duplicataNoBanco) {
                    salvarPosicaoNoBanco(posicao, 0); // valor 0 = deletar a linha
                    System.out.println("Duplicata do pedido " + pedidoId
                            + " removida do banco na posição " + posicao + ".");
                }

                // Remove duplicata na memória do CLP (apenas se a word contém ESTE pedido).
                Integer valorNoClp = lerPosicaoNoClp(ipClp, posicao);
                if (valorNoClp != null && valorNoClp == pedidoId) {
                    boolean limpo = escreverPosicaoNoClp(ipClp, posicao, 0);
                    if (limpo) {
                        System.out.println("Duplicata do pedido " + pedidoId
                                + " removida do CLP na posição " + posicao + ".");
                    }
                }
                // Se a posição contém outro pedido (ou está vazia), NÃO TOCAMOS nela.
            }

            return ResponseEntity.ok("Reconciliação da expedição concluída: pedido " + pedidoId
                    + " mantido apenas na posição " + posicaoCorreta + ".");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao corrigir duplicidade da expedição: " + e.getMessage());
        }
    }

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

    private Integer lerPosicaoNoClp(String ipClp, int posicao) {
        if (SmartService.readOnly) {
            return null; // em modo simulação não há o que ler/limpar
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

    private String asString(Object valor) {
        return valor == null ? null : valor.toString();
    }
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

        SmartService.posicaoExpedicaoSolicitada = posicao;

        return ResponseEntity.ok("Posição " + posicao + " preparada no CLP de Expedição.");
    } catch (Exception e) {
        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Erro ao preparar posição da expedição: " + e.getMessage());
    }
}

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

        plcConnector.writeBit(9, 2, 1, false);
        plcConnector.writeBit(9, 2, 0, false);
        plcConnector.writeInt(9, 4, posicao);

        return true;
    } catch (Exception e) {
        e.printStackTrace();
        return false;
    }
}
}