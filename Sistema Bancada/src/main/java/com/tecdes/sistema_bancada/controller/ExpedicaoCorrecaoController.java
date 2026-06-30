package com.tecdes.sistema_bancada.controller;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

            Set<Integer> posicoesParaCorrigir = new HashSet<>();
            posicoesParaCorrigir.add(posicaoCorreta);

            List<Expedicao> posicoesAtuais = expedicaoRepository.findAll();
            for (Expedicao exp : posicoesAtuais) {
                if (exp.getOrderNumber() == pedidoId && exp.getPosicaoExpedicao() != posicaoCorreta) {
                    posicoesParaCorrigir.add(exp.getPosicaoExpedicao());
                }
            }

            for (Integer posicao : posicoesParaCorrigir) {
                if (posicao == null || posicao < 1 || posicao > 12) {
                    continue;
                }

                int valorCorreto = (posicao == posicaoCorreta)
                        ? pedidoId
                        : valorSnapshot(snapshot, posicao);

                salvarPosicaoNoBanco(posicao, valorCorreto);

                boolean escrito = escreverPosicaoNoClp(ipClp, posicao, valorCorreto);
                if (!escrito) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body("Falha ao corrigir posição " + posicao + " no CLP de Expedição.");
                }
            }

            return ResponseEntity.ok("Correção de duplicidade da expedição concluída.");
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
            System.out.println("Corrigindo expedição - posição " + posicao + " offset DB9:" + offset + " valor:" + valor);
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
}
