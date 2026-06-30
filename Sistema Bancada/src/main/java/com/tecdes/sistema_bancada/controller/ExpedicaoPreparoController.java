package com.tecdes.sistema_bancada.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.tecdes.sistema_bancada.service.PlcConnector;
import com.tecdes.sistema_bancada.service.SmartService;
import com.tecdes.sistema_bancada.service.SmartService.PlcConnectionManager;

@RestController
public class ExpedicaoPreparoController {

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

            // Limpa as flags de recebido/iniciar guardar da expedição e atualiza a posição usada pelo CLP.
            // Isso evita que a rotina antiga continue usando uma posição velha, como a posição 1.
            plcConnector.writeBit(9, 2, 1, false);
            plcConnector.writeBit(9, 2, 0, false);
            plcConnector.writeInt(9, 4, posicao);

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
}
