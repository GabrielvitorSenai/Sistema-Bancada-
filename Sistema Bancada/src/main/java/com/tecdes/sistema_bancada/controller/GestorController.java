package com.tecdes.sistema_bancada.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tecdes.sistema_bancada.model.Estoque;
import com.tecdes.sistema_bancada.model.Expedicao;
import com.tecdes.sistema_bancada.repository.EstoqueRepository;
import com.tecdes.sistema_bancada.repository.ExpedicaoRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Gestão - Estoque e Expedição",
        description = "Leitura das posições do magazine de estoque e do magazine de expedição.")
public class GestorController {

    @Autowired
    private EstoqueRepository estoqueRepository;

    @Autowired
    private ExpedicaoRepository expedicaoRepository;

    @Operation(summary = "Cores dos blocos no estoque",
            description = "Retorna a cor de cada posição do magazine de estoque (P1..P28).")
    @GetMapping("/blocos-estoque")
    public Map<String, Integer> getValores() {
        List<Estoque> lista = estoqueRepository.findAll(Sort.by("posicaoEstoque")); // ordenado por posição
        Map<String, Integer> valores = new LinkedHashMap<>();

        for (Estoque est : lista) {
            String chave = "P" + est.getPosicaoEstoque(); // exemplo: posicao_1, posicao_2...
            valores.put(chave, est.getCor());
        }

        return valores;
    }
    
    @Operation(summary = "Pedidos guardados na expedição",
            description = "Retorna o número de OP guardado em cada posição do magazine de expedição (P1..P12).")
    @GetMapping("/pedidos-expedicao")
    public Map<String, Integer> carregarValoresExpedicao() {
        List<Expedicao> lista = expedicaoRepository.findAll(Sort.by(Sort.Direction.ASC, "posicaoExpedicao"));

        Map<String, Integer> valores = new LinkedHashMap<>();

        for (Expedicao exp : lista) {
            String chave = "P" + exp.getPosicaoExpedicao(); // Ex: posicao_1, posicao_2...
            valores.put(chave, exp.getOrderNumber());
        }

        return valores;
    }
}
