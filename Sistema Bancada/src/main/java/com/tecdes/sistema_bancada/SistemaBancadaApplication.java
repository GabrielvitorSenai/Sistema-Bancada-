package com.tecdes.sistema_bancada;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Classe principal da aplicação.
 *
 * O Spring Boot começa a execução do sistema por aqui. A anotação
 * @SpringBootApplication habilita a configuração automática do Spring,
 * o escaneamento dos controllers, services, repositories e demais componentes
 * dentro do pacote com.tecdes.sistema_bancada.
 */
@SpringBootApplication
public class SistemaBancadaApplication {

	/**
	 * Método main executado pela JVM.
	 *
	 * Ele inicializa o contexto Spring, sobe o servidor Tomcat embutido
	 * e deixa disponíveis as telas, endpoints REST e serviços do sistema.
	 */
	public static void main(String[] args) {
		SpringApplication.run(SistemaBancadaApplication.class, args);
	}

}
