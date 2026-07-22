package com.tecdes.sistema_bancada;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifica a entrega de Documentação da API (Swagger / OpenAPI).
 *
 * Confirma que o springdoc-openapi expõe o documento OpenAPI em /v3/api-docs,
 * que o cabeçalho personalizado ({@code OpenApiConfig}) está presente e que os
 * principais endpoints de pedidos aparecem documentados. Serve como garantia
 * automatizada de que o Swagger continua funcionando após futuras mudanças.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SwaggerDocumentacaoTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("O documento OpenAPI é publicado em /v3/api-docs com o título e os endpoints de pedidos")
    void documentoOpenApiDisponivel() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"openapi\"")))
                .andExpect(jsonPath("$.info.title", containsString("Planta SMART 4.0")))
                .andExpect(jsonPath("$.paths['/api/pedidos']").exists())
                .andExpect(jsonPath("$.paths['/api/pedidos/{id}/status']").exists());
    }
}
