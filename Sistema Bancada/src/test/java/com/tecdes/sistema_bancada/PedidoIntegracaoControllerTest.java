package com.tecdes.sistema_bancada;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tecdes.sistema_bancada.dto.BlocoDTO;
import com.tecdes.sistema_bancada.dto.LaminaDTO;
import com.tecdes.sistema_bancada.dto.PedidoDTO;
import com.tecdes.sistema_bancada.model.Estoque;
import com.tecdes.sistema_bancada.repository.EstoqueRepository;

/**
 * Testes de INTEGRAÇÃO do fluxo completo Controller -> Service -> Banco (H2).
 *
 * Sobe o contexto Spring inteiro ({@link SpringBootTest}) contra o banco H2 em
 * memória (configurado em src/test/resources/application.properties) e exercita
 * os endpoints REST via {@link MockMvc}, como um operador faria pela Loja.
 *
 * Cada teste roda dentro de uma transação que é revertida ao final
 * ({@link Transactional}), garantindo isolamento entre os cenários.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PedidoIntegracaoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EstoqueRepository estoqueRepository;

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private LaminaDTO lamina(int cor, int padrao) {
        LaminaDTO l = new LaminaDTO();
        l.setCor(cor);
        l.setPadrao(padrao);
        return l;
    }

    private BlocoDTO bloco(int andar, int cor, LaminaDTO... laminas) {
        BlocoDTO b = new BlocoDTO();
        b.setAndar(andar);
        b.setCorBloco(cor);
        List<LaminaDTO> lista = new ArrayList<>();
        for (LaminaDTO l : laminas) {
            lista.add(l);
        }
        b.setLaminas(lista);
        return b;
    }

    private PedidoDTO pedido(String tipo, int tampa, BlocoDTO... blocos) {
        PedidoDTO dto = new PedidoDTO();
        dto.setTipo(tipo);
        dto.setTampa(tampa);
        dto.setStatusOrderProduction("pendente");
        List<BlocoDTO> lista = new ArrayList<>();
        for (BlocoDTO b : blocos) {
            lista.add(b);
        }
        dto.setBlocos(lista);
        return dto;
    }

    private String json(Object o) throws Exception {
        return objectMapper.writeValueAsString(o);
    }

    private void seedEstoque(int qtdPreto, int qtdVermelho, int qtdAzul) {
        int pos = 1;
        for (int i = 0; i < qtdPreto; i++) {
            salvarPosicao(pos++, 1);
        }
        for (int i = 0; i < qtdVermelho; i++) {
            salvarPosicao(pos++, 2);
        }
        for (int i = 0; i < qtdAzul; i++) {
            salvarPosicao(pos++, 3);
        }
    }

    private void salvarPosicao(int posicao, int cor) {
        Estoque e = new Estoque();
        e.setPosicaoEstoque(posicao);
        e.setCor(cor);
        estoqueRepository.save(e);
    }

    // ------------------------------------------------------------------
    // Fluxo de sucesso: pedido "duplo" com estoque suficiente
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Operador cria um pedido DUPLO (2 blocos) com estoque disponível e ele entra em produção")
    void criaPedidoDuploComEstoqueEEntraEmProducao() throws Exception {
        // Estoque com exatamente 2 blocos azuis, o necessário para o pedido duplo.
        seedEstoque(0, 0, 2);

        PedidoDTO duplo = pedido("duplo", 1, bloco(1, 3, lamina(1, 1)), bloco(2, 3, lamina(2, 2)));

        // 1) Cria o pedido: 201 Created e 2 blocos gravados.
        String corpoCriacao = mockMvc.perform(post("/api/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(duplo)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.tipo", is("duplo")))
                .andExpect(jsonPath("$.statusOrderProduction", is("pendente")))
                .andExpect(jsonPath("$.blocos.length()", is(2)))
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(corpoCriacao).get("id").asLong();

        // 2) O pedido aparece na listagem geral.
        mockMvc.perform(get("/api/pedidos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + id + ")]").exists());

        // 3) O operador coloca o pedido em produção: a mudança de status é refletida.
        mockMvc.perform(put("/api/pedidos/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"statusOrderProduction\":\"producao\"}"))
                .andExpect(status().isOk());

        // 4) A consulta reflete o pedido agora "em produção" (visível no filtro Produção).
        mockMvc.perform(get("/listar-pedido/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusOrderProduction", is("producao")));
    }

    // ------------------------------------------------------------------
    // O sistema impede a criação de um pedido sem estoque suficiente
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Impede criar pedido duplo quando não há blocos suficientes no estoque (422)")
    void impedeCriacaoComEstoqueInsuficiente() throws Exception {
        // Só 1 bloco azul disponível, mas o pedido duplo precisa de 2.
        seedEstoque(0, 0, 1);

        PedidoDTO duplo = pedido("duplo", 1, bloco(1, 3), bloco(2, 3));

        mockMvc.perform(post("/api/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(duplo)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().string(containsString("Estoque insuficiente")));
    }

    // ------------------------------------------------------------------
    // Regras de Ouro rejeitadas na borda HTTP
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Rejeita cor de tampa fora do intervalo 1-3 (422)")
    void rejeitaTampaInvalida() throws Exception {
        PedidoDTO invalido = pedido("simples", 9, bloco(1, 1));

        mockMvc.perform(post("/api/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(invalido)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().string(containsString("tampa")));
    }

    @Test
    @DisplayName("Rejeita bloco com 4 lâminas, respeitando o limite de 3 (422)")
    void rejeitaBlocoComQuatroLaminas() throws Exception {
        BlocoDTO blocoComQuatro = bloco(1, 1,
                lamina(1, 0), lamina(2, 0), lamina(3, 0), lamina(4, 0));
        PedidoDTO invalido = pedido("simples", 1, blocoComQuatro);

        mockMvc.perform(post("/api/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(invalido)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().string(containsString("máximo 3 lâminas")));
    }

    @Test
    @DisplayName("Rejeita número de pedido duplicado (409)")
    void rejeitaNumeroDuplicado() throws Exception {
        seedEstoque(5, 5, 5);

        PedidoDTO primeiro = pedido("simples", 1, bloco(1, 3));
        primeiro.setNumeroPedido("2000");
        mockMvc.perform(post("/api/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(primeiro)))
                .andExpect(status().isCreated());

        PedidoDTO segundo = pedido("simples", 1, bloco(1, 3));
        segundo.setNumeroPedido("2000");
        mockMvc.perform(post("/api/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(segundo)))
                .andExpect(status().isConflict());
    }

    // ------------------------------------------------------------------
    // A interface acompanha as etapas até a finalização
    // ------------------------------------------------------------------

    @Test
    @DisplayName("O status percorre pendente -> produção -> concluído e cada etapa é refletida na consulta")
    void statusPercorreEtapasAteConclusao() throws Exception {
        seedEstoque(3, 0, 0);

        PedidoDTO simples = pedido("simples", 1, bloco(1, 1));
        String corpo = mockMvc.perform(post("/api/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(simples)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = objectMapper.readTree(corpo).get("id").asLong();

        // Etapa 2 - produção (aceita também o código numérico "2").
        mockMvc.perform(put("/api/pedidos/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"statusOrderProduction\":\"2\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/listar-pedido/" + id))
                .andExpect(jsonPath("$.statusOrderProduction", is("producao")));

        // Etapa 3 - concluído (código numérico "3").
        mockMvc.perform(put("/api/pedidos/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"statusOrderProduction\":\"3\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/listar-pedido/" + id))
                .andExpect(jsonPath("$.statusOrderProduction", is("concluido")));
    }
}
