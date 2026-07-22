package com.tecdes.sistema_bancada.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;

import java.util.List;

/**
 * Metadados da documentação OpenAPI/Swagger da API da planta SMART 4.0.
 *
 * O starter springdoc-openapi já gera a especificação a partir dos controllers;
 * esta classe apenas personaliza o cabeçalho exibido no Swagger UI
 * (/swagger-ui.html) e no documento JSON (/v3/api-docs), como pede a entrega
 * de Documentação da API da Etapa 4.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI sistemaBancadaOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API - Sistema de Gerenciamento da Planta SMART 4.0")
                        .description("Endpoints REST do painel operacional da linha didática 4.0: "
                                + "criação e gestão de pedidos, controle de estoque (magazine), "
                                + "expedição e integração com a bancada. "
                                + "As regras de negócio (Regras de Ouro) são validadas na camada de "
                                + "Service e retornam mensagens claras ao operador.")
                        .version("v1")
                        .contact(new Contact()
                                .name("Equipe TecDes - Planta SMART 4.0"))
                        .license(new License()
                                .name("Uso didático - SENAI")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8088")
                                .description("Ambiente local de desenvolvimento")));
    }
}
