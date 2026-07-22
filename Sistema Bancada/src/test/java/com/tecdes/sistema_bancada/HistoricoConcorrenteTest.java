package com.tecdes.sistema_bancada;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.tecdes.sistema_bancada.dto.BlocoDTO;
import com.tecdes.sistema_bancada.dto.LaminaDTO;
import com.tecdes.sistema_bancada.dto.PedidoDTO;
import com.tecdes.sistema_bancada.model.Pedido;
import com.tecdes.sistema_bancada.repository.BlocoRepository;
import com.tecdes.sistema_bancada.repository.PedidoRepository;
import com.tecdes.sistema_bancada.service.PedidoService;

/**
 * Teste de estresse de concorrência: garante que o banco dbSmart40 mantém o
 * histórico correto mesmo após múltiplas operações SIMULTÂNEAS.
 *
 * Vários "operadores" (threads) criam pedidos ao mesmo tempo. Ao final,
 * verifica-se que todos os pedidos foram persistidos, cada um com um ID único e
 * com o número de produção (orderProduction) corretamente vinculado ao seu ID —
 * ou seja, nenhum registro se perdeu nem se misturou com outro.
 *
 * Não usa @Transactional: cada criação precisa efetivar (commit) sua própria
 * transação para simular gravações concorrentes reais.
 */
@SpringBootTest
class HistoricoConcorrenteTest {

    private static final int TOTAL_PEDIDOS = 20;

    @Autowired
    private PedidoService pedidoService;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private BlocoRepository blocoRepository;

    @BeforeEach
    void limparAntes() {
        pedidoRepository.deleteAll();
    }

    @AfterEach
    void limparDepois() {
        pedidoRepository.deleteAll();
    }

    private PedidoDTO novoPedidoSimples(int indice) {
        LaminaDTO lamina = new LaminaDTO();
        lamina.setCor(0);
        lamina.setPadrao(0);

        BlocoDTO bloco = new BlocoDTO();
        bloco.setAndar(1);
        bloco.setCorBloco(1);
        List<LaminaDTO> laminas = new ArrayList<>();
        laminas.add(lamina);
        bloco.setLaminas(laminas);

        PedidoDTO dto = new PedidoDTO();
        dto.setTipo("simples");
        dto.setTampa(1);
        dto.setStatusOrderProduction("pendente");
        // Número de pedido único por operador, para também exercitar a validação de unicidade.
        dto.setNumeroPedido("OP-" + indice);
        List<BlocoDTO> blocos = new ArrayList<>();
        blocos.add(bloco);
        dto.setBlocos(blocos);
        return dto;
    }

    @Test
    @DisplayName("Criações simultâneas preservam todos os registros, sem perdas nem IDs duplicados")
    void criacoesSimultaneasMantemHistoricoIntegro() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(TOTAL_PEDIDOS);
        CountDownLatch largada = new CountDownLatch(1);
        CountDownLatch chegada = new CountDownLatch(TOTAL_PEDIDOS);
        List<Long> idsCriados = new CopyOnWriteArrayList<>();
        List<Exception> falhas = new CopyOnWriteArrayList<>();

        for (int i = 0; i < TOTAL_PEDIDOS; i++) {
            final int indice = i;
            executor.submit(() -> {
                try {
                    largada.await(); // Todos esperam a "largada" para agir ao mesmo tempo.
                    Pedido salvo = pedidoService.criarPedido(novoPedidoSimples(indice));
                    idsCriados.add(salvo.getId());
                } catch (Exception e) {
                    falhas.add(e);
                } finally {
                    chegada.countDown();
                }
            });
        }

        largada.countDown(); // Dispara todas as criações simultaneamente.
        boolean terminou = chegada.await(30, TimeUnit.SECONDS);
        executor.shutdownNow();

        assertThat(terminou).as("todas as threads terminaram dentro do tempo").isTrue();
        assertThat(falhas).as("nenhuma criação simultânea deve falhar").isEmpty();

        // Nenhum registro se perdeu: o banco tem exatamente os pedidos criados.
        List<Pedido> persistidos = pedidoRepository.findAll();
        assertThat(persistidos).hasSize(TOTAL_PEDIDOS);

        // Todos os IDs são únicos (não houve mistura de registros).
        assertThat(idsCriados).doesNotHaveDuplicates().hasSize(TOTAL_PEDIDOS);

        // O histórico está coerente: para cada pedido, orderProduction == id.
        for (Pedido p : persistidos) {
            assertThat(p.getOrderProduction()).isEqualTo(p.getId().intValue());
        }

        // Cada pedido gravou seu bloco (1 por pedido), totalizando TOTAL_PEDIDOS blocos.
        // Contamos via repositório para não depender de sessão Hibernate aberta.
        assertThat(blocoRepository.count()).isEqualTo(TOTAL_PEDIDOS);

        // Os números de pedido informados (OP-0 .. OP-19) estão todos gravados, sem repetição.
        List<String> numeros = persistidos.stream()
                .map(Pedido::getNumeroPedido)
                .collect(Collectors.toList());
        assertThat(numeros).doesNotHaveDuplicates().hasSize(TOTAL_PEDIDOS);
    }
}
