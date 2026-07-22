package com.tecdes.sistema_bancada.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

/**
 * DataSource montado manualmente para o banco MySQL de produção (dbSmart40).
 *
 * Fica ativo por padrão (produção/desenvolvimento local). Nos testes
 * automatizados a propriedade {@code app.datasource.manual.enabled=false}
 * (definida em src/test/resources/application.properties) desliga esta
 * configuração, deixando o Spring Boot autoconfigurar o banco H2 em memória.
 * Assim os testes rodam sem depender de um MySQL de produção, como pede a
 * Etapa 4 (banco H2 em memória para não poluir o MySQL).
 */
@Configuration
@ConditionalOnProperty(name = "app.datasource.manual.enabled", havingValue = "true", matchIfMissing = true)
public class DataSourceManualConfig {

    @Autowired
    private Environment environment;

    @Bean
    @Primary
    public DataSource dataSource() {
        return DataSourceBuilder.create()
                .driverClassName("com.mysql.cj.jdbc.Driver")
                .url(environment.getProperty("spring.datasource.url"))
                .username(environment.getProperty("spring.datasource.username"))
                .password(environment.getProperty("spring.datasource.password"))
                .build();
    }
}
