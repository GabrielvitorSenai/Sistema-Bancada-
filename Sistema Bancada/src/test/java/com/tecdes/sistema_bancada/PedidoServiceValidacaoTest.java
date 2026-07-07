package com.tecdes.sistema_bancada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;

import com.tecdes.sistema_bancada.dto.BlocoDTO;
import com.tecdes.sistema_bancada.dto.LaminaDTO;
import com.tecdes.sistema_bancada.dto.PedidoDTO;
import com.tecdes.sistema_bancada.exception.BusinessException;
import com.tecdes.sistema_bancada.model.Estoque;
import com.tecdes.sistema_bancada.model.Pedido;
import com.tecdes.sistema_bancada.repository.EstoqueRepository;
import com.tecdes.sistema_bancada.service.PedidoService;

/**
 * Verifica as Regras de Ouro validadas na camada de Service (Etapa 2) e que o
 * caminho feliz de criação de pedido continua funcionando — para garantir que a
 * validação nova não quebra o fluxo normal da Loja.
 */
@DataJpaTest
@Import(PedidoService.class)
class PedidoServiceValidacaoTest {

    @Autowired
    private PedidoService pedidoService;

    @Autowired
    private EstoqueRepository estoqueRepository;

    private LaminaDTO lamina(int cor, int padrao) {
        LaminaDTO l = new LaminaDTO();
        l.setCor(cor);
        l.setPadrao(padrao);
        return l;
    }

    private BlocoDTO bloco(int andar, int cor) {
        BlocoDTO b = new BlocoDTO();
        b.setAndar(andar);
        b.setCorBloco(cor);
        List<LaminaDTO> laminas = new ArrayList<>();
        laminas.add(lamina(0, 0));
        laminas.add(lamina(0, 0));
        laminas.add(lamina(0, 0));
        b.setLaminas(laminas);
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

    private void popularEstoque(int qtdPreto, int qtdVermelho, int qtdAzul) {
        int pos = 1;
        for (int i = 0; i < qtdPreto; i++) {
            salvarEstoque(pos++, 1);
        }
        for (int i = 0; i < qtdVermelho; i++) {
            salvarEstoque(pos++, 2);
        }
        for (int i = 0; i < qtdAzul; i++) {
            salvarEstoque(pos++, 3);
        }
    }

    private void salvarEstoque(int posicao, int cor) {
        Estoque e = new Estoque();
        e.setPosicaoEstoque(posicao);
        e.setCor(cor);
        estoqueRepository.save(e);
    }

    @Test
    void criaPedidoSimplesNoCaminhoFeliz() {
        popularEstoque(5, 5, 5);
        Pedido salvo = pedidoService.criarPedido(pedido("simples", 1, bloco(1, 3)));

        assertThat(salvo.getId()).isNotNull();
        assertThat(salvo.getOrderProduction()).isEqualTo(salvo.getId().intValue());
        assertThat(salvo.getBlocos()).hasSize(1);
        assertThat(salvo.getStatusOrderProduction()).isEqualTo("pendente");
    }

    @Test
    void recusaTriploComNumeroDeBlocosErrado() {
        popularEstoque(5, 5, 5);
        assertThatThrownBy(() -> pedidoService.criarPedido(pedido("triplo", 1, bloco(1, 1), bloco(2, 1))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("exatamente 3");
    }

    @Test
    void recusaCorDeTampaInexistente() {
        popularEstoque(5, 5, 5);
        assertThatThrownBy(() -> pedidoService.criarPedido(pedido("simples", 9, bloco(1, 1))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("tampa");
    }

    @Test
    void recusaEstoqueInsuficiente() {
        popularEstoque(0, 0, 1); // só 1 azul disponível
        // triplo pedindo 3 blocos azuis não cabe no estoque
        assertThatThrownBy(() -> pedidoService.criarPedido(
                pedido("triplo", 1, bloco(1, 3), bloco(2, 3), bloco(3, 3))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Estoque insuficiente");
    }

    @Test
    void permiteQuandoEstoqueAtende() {
        popularEstoque(0, 0, 3);
        assertThatCode(() -> pedidoService.criarPedido(
                pedido("triplo", 1, bloco(1, 3), bloco(2, 3), bloco(3, 3))))
                .doesNotThrowAnyException();
    }

    @Test
    void naoBloqueiaQuandoEstoqueNaoConfigurado() {
        // Sem nenhuma posição de estoque cadastrada, a validação não bloqueia.
        assertThatCode(() -> pedidoService.criarPedido(pedido("simples", 1, bloco(1, 3))))
                .doesNotThrowAnyException();
    }

    @Test
    void recusaNumeroDePedidoDuplicado() {
        popularEstoque(5, 5, 5);
        PedidoDTO primeiro = pedido("simples", 1, bloco(1, 3));
        primeiro.setNumeroPedido("1000");
        pedidoService.criarPedido(primeiro);

        PedidoDTO segundo = pedido("simples", 1, bloco(1, 3));
        segundo.setNumeroPedido("1000");
        assertThatThrownBy(() -> pedidoService.criarPedido(segundo))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void recusaEdicaoDePedidoNaoPendente() {
        popularEstoque(5, 5, 5);
        Pedido salvo = pedidoService.criarPedido(pedido("simples", 1, bloco(1, 3)));
        pedidoService.atualizarStatus(salvo.getId(), "concluido");

        PedidoDTO edicao = pedido("simples", 2, bloco(1, 3));
        assertThatThrownBy(() -> pedidoService.editarPedido(salvo.getId(), edicao))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }
}
