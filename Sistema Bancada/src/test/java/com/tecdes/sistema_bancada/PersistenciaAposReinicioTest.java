package com.tecdes.sistema_bancada;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import com.tecdes.sistema_bancada.model.Bloco;
import com.tecdes.sistema_bancada.model.Pedido;
import com.tecdes.sistema_bancada.repository.BlocoRepository;
import com.tecdes.sistema_bancada.repository.PedidoRepository;

/**
 * Teste de PERSISTÊNCIA: comprova que os dados sobrevivem a um "reinício" da
 * aplicação, garantindo que o mapeamento JPA está gravando de fato no banco.
 *
 * Diferente dos demais testes (que usam H2 em memória), aqui a aplicação inteira
 * é iniciada DUAS vezes contra um banco H2 em ARQUIVO:
 *   1) Primeiro boot: grava um pedido e encerra a aplicação (fecha o contexto).
 *   2) Segundo boot: reabre a aplicação e confirma que o pedido continua lá,
 *      com seus blocos, exatamente como no cenário "Reiniciar a aplicação e
 *      verificar se os dados cadastrados permanecem".
 *
 * O banco em arquivo com ddl-auto=update evita que o esquema/dados sejam
 * descartados entre os dois boots.
 */
class PersistenciaAposReinicioTest {

    // Banco em arquivo dedicado a este teste, sob target/ (ignorado pelo Git).
    private static final Path DB_DIR = Paths.get("target", "persist-test");
    private static final String DB_URL =
            "jdbc:h2:file:./target/persist-test/dbSmart40;MODE=MySQL;DATABASE_TO_LOWER=TRUE";

    private ConfigurableApplicationContext iniciarAplicacao() {
        return new SpringApplicationBuilder(SistemaBancadaApplication.class)
                .web(WebApplicationType.NONE)
                .run(
                        "--app.datasource.manual.enabled=false",
                        "--spring.datasource.url=" + DB_URL,
                        "--spring.datasource.driver-class-name=org.h2.Driver",
                        "--spring.datasource.username=sa",
                        "--spring.datasource.password=",
                        "--spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
                        // update (e não create-drop) preserva esquema e dados entre os boots.
                        "--spring.jpa.hibernate.ddl-auto=update",
                        "--db.config.path=");
    }

    private void apagarBancoAnterior() throws IOException {
        if (!Files.exists(DB_DIR)) {
            return;
        }
        try (var caminhos = Files.walk(DB_DIR)) {
            caminhos.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    @Test
    @DisplayName("Um pedido gravado antes do reinício continua no banco após reiniciar a aplicação")
    void dadosPersistemAposReiniciarAplicacao() throws Exception {
        apagarBancoAnterior();

        Long idGerado;
        String numeroPedido = "PERSIST-1";

        // ---- 1º boot: grava o pedido e encerra a aplicação ----
        try (ConfigurableApplicationContext contexto1 = iniciarAplicacao()) {
            PedidoRepository repositorio = contexto1.getBean(PedidoRepository.class);

            Pedido pedido = new Pedido();
            pedido.setNumeroPedido(numeroPedido);
            pedido.setTipo("duplo");
            pedido.setTampa(2);
            pedido.setStatusOrderProduction("pendente");

            List<Bloco> blocos = new ArrayList<>();
            Bloco b1 = new Bloco();
            b1.setCor(1);
            Bloco b2 = new Bloco();
            b2.setCor(3);
            blocos.add(b1);
            blocos.add(b2);
            pedido.setBlocos(blocos);

            Pedido salvo = repositorio.save(pedido);
            idGerado = salvo.getId();
            assertThat(idGerado).isNotNull();
        }
        // Ao sair do try-with-resources o contexto é fechado: a aplicação "desligou".

        // ---- 2º boot: reabre a aplicação e confere que o dado permanece ----
        try (ConfigurableApplicationContext contexto2 = iniciarAplicacao()) {
            PedidoRepository repositorio = contexto2.getBean(PedidoRepository.class);
            BlocoRepository blocoRepositorio = contexto2.getBean(BlocoRepository.class);

            Pedido recuperado = repositorio.findById(idGerado).orElse(null);

            assertThat(recuperado)
                    .as("o pedido gravado antes do reinício deve continuar no banco")
                    .isNotNull();
            assertThat(recuperado.getNumeroPedido()).isEqualTo(numeroPedido);
            assertThat(recuperado.getTipo()).isEqualTo("duplo");
            assertThat(recuperado.getTampa()).isEqualTo(2);
            // Os 2 blocos do pedido também sobreviveram ao reinício (banco recém-criado
            // no início do teste, então a contagem total reflete apenas este pedido).
            assertThat(blocoRepositorio.count()).isEqualTo(2);
        }
    }
}
