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
            Map<?, ?> snapshot = payload.get("snapshot") instanceof Map<?, ?> map ? map : Map.of();

            if (pedidoId <= 0) {
                return ResponseEntity.badRequest().body("Pedido inválido para correção da expedição.");
            }

            if (posicaoCorreta < 1 || posicaoCorreta > 12) {
                return ResponseEntity.badRequest().body("Posição correta inválida para correção da expedição.");
            }

            if (ipClp == null || ipClp.isBlank()) {
                return ResponseEntity.badRequest().body("IP do CLP de Expedição não informado.");
            }

            // Reconciliamos as 12 posições com a foto tirada antes da execução.
            // Regra: somente a posição correta recebe o pedido novo; todas as outras voltam ao valor anterior.
            // Isso corrige tanto duplicidade no banco quanto duplicidade que ficou só na memória do CLP.
            for (int posicao = 1; posicao <= 12; posicao++) {
                int valorCorreto = (posicao == posicaoCorreta)
                        ? pedidoId
                        : valorSnapshot(snapshot, posicao);

                salvarPosicaoNoBanco(posicao, valorCorreto);

                boolean escrito = escreverPosicaoNoClp(ipClp, posicao, valorCorreto);
                if (!escrito) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body("Falha ao reconciliar posição " + posicao + " no CLP de Expedição.");
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

    private int valorSnapshot(Map<?, ?> snapshot, int posicao) {
        Object valor = snapshot.get(String.valueOf(posicao));
        if (valor == null) {
            valor = snapshot.get("P" + posicao);
        }
        if (valor == null) {
            valor = snapshot.get(posicao);
        }
        return asInt(valor);
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

