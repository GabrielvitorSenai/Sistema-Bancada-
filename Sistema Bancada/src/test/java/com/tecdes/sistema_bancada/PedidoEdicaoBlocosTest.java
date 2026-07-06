package com.tecdes.sistema_bancada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.tecdes.sistema_bancada.model.Bloco;
import com.tecdes.sistema_bancada.model.Pedido;
import com.tecdes.sistema_bancada.repository.PedidoRepository;

import jakarta.persistence.EntityManager;

/**
 * Regressão para o erro 500 reportado ao editar um pedido (PUT /api/pedidos/{id}).
 *
 * Causa raiz: Pedido.blocos usa orphanRemoval=true. Ao editar um pedido já
 * persistido, substituir a referência da coleção gerenciada por uma lista nova
 * (pedido.setBlocos(novaLista)) faz o Hibernate perder o rastreamento da
 * coleção original e lançar HibernateException no flush. A correção é limpar a
 * coleção existente e adicionar os novos itens nela (ver
 * LojaController.substituirBlocos).
 */
@DataJpaTest
class PedidoEdicaoBlocosTest {

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private EntityManager entityManager;

    // Cria o pedido e limpa o contexto de persistência, simulando o que
    // acontece de fato em produção: a criação (POST) e a edição (PUT) do
    // pedido ocorrem em requisições HTTP diferentes, cada uma com sua própria
    // sessão/EntityManager (mesmo com Open Session In View).
    private Long criarPedidoComUmBlocoEmNovaTransacao() {
        Pedido pedido = new Pedido();
        pedido.setTipo("simples");
        pedido.setTampa(1);
        pedido.setStatusOrderProduction("pendente");

        Bloco bloco = new Bloco();
        bloco.setCor(1);
        pedido.setBlocos(new ArrayList<>(List.of(bloco)));

        pedido = pedidoRepository.saveAndFlush(pedido);
        Long id = pedido.getId();

        entityManager.flush();
        entityManager.clear();

        return id;
    }

    @Test
    void substituirAReferenciaDaColecaoQuebraOFlush() {
        Long id = criarPedidoComUmBlocoEmNovaTransacao();
        Pedido pedido = pedidoRepository.findById(id).orElseThrow();

        Bloco novoBloco = new Bloco();
        novoBloco.setCor(2);

        // Reproduz o bug relatado: troca a referência da lista mapeada.
        pedido.setBlocos(List.of(novoBloco));

        assertThrows(Exception.class, () -> pedidoRepository.saveAndFlush(pedido));
    }

    @Test
    void limparEAdicionarNaMesmaColecaoFunciona() {
        Long id = criarPedidoComUmBlocoEmNovaTransacao();
        Pedido pedido = pedidoRepository.findById(id).orElseThrow();

        Bloco novoBloco = new Bloco();
        novoBloco.setCor(2);
        novoBloco.setPedido(pedido);

        // Correção usada em LojaController.substituirBlocos: reaproveita a coleção.
        pedido.getBlocos().clear();
        pedido.getBlocos().add(novoBloco);

        Pedido salvo = pedidoRepository.saveAndFlush(pedido);

        assertThat(salvo.getBlocos()).hasSize(1);
        assertThat(salvo.getBlocos().get(0).getCor()).isEqualTo(2);
    }
}
