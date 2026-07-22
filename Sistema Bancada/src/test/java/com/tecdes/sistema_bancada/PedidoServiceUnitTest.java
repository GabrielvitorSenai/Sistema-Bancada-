package com.tecdes.sistema_bancada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.tecdes.sistema_bancada.dto.BlocoDTO;
import com.tecdes.sistema_bancada.dto.LaminaDTO;
import com.tecdes.sistema_bancada.dto.PedidoDTO;
import com.tecdes.sistema_bancada.exception.BusinessException;
import com.tecdes.sistema_bancada.model.Pedido;
import com.tecdes.sistema_bancada.repository.EstoqueRepository;
import com.tecdes.sistema_bancada.repository.PedidoRepository;
import com.tecdes.sistema_bancada.service.PedidoService;

/**
 * Testes UNITÁRIOS da camada de Service (JUnit 5 + Mockito).
 *
 * Aqui não há banco de dados nem contexto Spring: {@link PedidoRepository} e
 * {@link EstoqueRepository} são simulados (mock), então cada teste valida
 * exclusivamente a lógica das "Regras de Ouro" dentro do {@link PedidoService}.
 *
 * Cobre os exemplos pedidos no roteiro da Etapa 4:
 *  - a cor da tampa fora do intervalo 1..3 deve ser recusada;
 *  - um bloco com 4 lâminas deve lançar exceção (limite máximo de 3).
 */
@ExtendWith(MockitoExtension.class)
class PedidoServiceUnitTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private EstoqueRepository estoqueRepository;

    @InjectMocks
    private PedidoService pedidoService;

    // ------------------------------------------------------------------
    // Fábricas de DTO para deixar os testes legíveis
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

    // ------------------------------------------------------------------
    // Cor da tampa (deve estar entre 1 e 3)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Recusa cor de tampa abaixo do intervalo (0)")
    void recusaTampaAbaixoDoIntervalo() {
        assertThatThrownBy(() -> pedidoService.validarComposicao(pedido("simples", 0, bloco(1, 1))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("tampa");
    }

    @Test
    @DisplayName("Recusa cor de tampa acima do intervalo (4)")
    void recusaTampaAcimaDoIntervalo() {
        assertThatThrownBy(() -> pedidoService.validarComposicao(pedido("simples", 4, bloco(1, 1))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("tampa");
    }

    @Test
    @DisplayName("Aceita as três cores de tampa válidas (1, 2 e 3)")
    void aceitaTampasValidas() {
        // Sem estoque configurado, a validação de disponibilidade não bloqueia.
        when(estoqueRepository.findAll()).thenReturn(new ArrayList<>());
        for (int tampa = 1; tampa <= 3; tampa++) {
            final int t = tampa;
            assertThatCode(() -> pedidoService.validarComposicao(pedido("simples", t, bloco(1, 1))))
                    .doesNotThrowAnyException();
        }
    }

    // ------------------------------------------------------------------
    // Limite de lâminas por bloco (máximo 3)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Recusa bloco com 4 lâminas (limite máximo é 3)")
    void recusaBlocoComQuatroLaminas() {
        BlocoDTO blocoComQuatro = bloco(1, 1,
                lamina(1, 0), lamina(2, 0), lamina(3, 0), lamina(4, 0));

        assertThatThrownBy(() -> pedidoService.validarComposicao(pedido("simples", 1, blocoComQuatro)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("máximo 3 lâminas");
    }

    @Test
    @DisplayName("Aceita bloco com exatamente 3 lâminas")
    void aceitaBlocoComTresLaminas() {
        when(estoqueRepository.findAll()).thenReturn(new ArrayList<>());
        BlocoDTO blocoComTres = bloco(1, 1, lamina(1, 1), lamina(2, 2), lamina(3, 3));

        assertThatCode(() -> pedidoService.validarComposicao(pedido("simples", 1, blocoComTres)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Recusa cor de lâmina fora do intervalo (7)")
    void recusaCorDeLaminaInvalida() {
        BlocoDTO blocoRuim = bloco(1, 1, lamina(7, 0));
        assertThatThrownBy(() -> pedidoService.validarComposicao(pedido("simples", 1, blocoRuim)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("lâmina");
    }

    @Test
    @DisplayName("Recusa padrão de lâmina fora do intervalo (4)")
    void recusaPadraoDeLaminaInvalido() {
        BlocoDTO blocoRuim = bloco(1, 1, lamina(1, 4));
        assertThatThrownBy(() -> pedidoService.validarComposicao(pedido("simples", 1, blocoRuim)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Padrão de lâmina");
    }

    // ------------------------------------------------------------------
    // Tipo do pedido x quantidade de blocos
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Recusa pedido triplo que não tenha exatamente 3 blocos")
    void recusaTriploComQuantidadeErrada() {
        assertThatThrownBy(() -> pedidoService.validarComposicao(
                pedido("triplo", 1, bloco(1, 1), bloco(2, 1))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("exatamente 3");
    }

    @Test
    @DisplayName("Recusa tipo de pedido inexistente")
    void recusaTipoInvalido() {
        assertThatThrownBy(() -> pedidoService.validarComposicao(
                pedido("quadruplo", 1, bloco(1, 1))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Tipo de pedido inválido");
    }

    @Test
    @DisplayName("Recusa pedido sem nenhum bloco")
    void recusaPedidoSemBlocos() {
        PedidoDTO semBlocos = pedido("simples", 1);
        assertThatThrownBy(() -> pedidoService.validarComposicao(semBlocos))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("pelo menos um bloco");
    }

    @Test
    @DisplayName("Recusa cor de bloco fora do intervalo (0)")
    void recusaCorDeBlocoInvalida() {
        assertThatThrownBy(() -> pedidoService.validarComposicao(pedido("simples", 1, bloco(1, 0))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cor de bloco inválida");
    }

    // ------------------------------------------------------------------
    // Estoque insuficiente (com repositório simulado)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Recusa pedido quando o estoque configurado não tem a cor pedida")
    void recusaQuandoEstoqueNaoTemACor() {
        // Estoque só com blocos pretos (cor 1); pedido pede um bloco azul (cor 3).
        List<com.tecdes.sistema_bancada.model.Estoque> estoque = new ArrayList<>();
        estoque.add(posicaoEstoque(1, 1));
        estoque.add(posicaoEstoque(2, 1));
        when(estoqueRepository.findAll()).thenReturn(estoque);

        assertThatThrownBy(() -> pedidoService.validarComposicao(pedido("simples", 1, bloco(1, 3))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Estoque insuficiente");
    }

    private com.tecdes.sistema_bancada.model.Estoque posicaoEstoque(int posicao, int cor) {
        com.tecdes.sistema_bancada.model.Estoque e = new com.tecdes.sistema_bancada.model.Estoque();
        e.setPosicaoEstoque(posicao);
        e.setCor(cor);
        return e;
    }

    // ------------------------------------------------------------------
    // Fluxo de criação (verificando a interação com o repositório mockado)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("criarPedido vincula o número de produção ao ID gerado e grava duas vezes")
    void criarPedidoVinculaOrderProductionAoId() {
        when(estoqueRepository.findAll()).thenReturn(new ArrayList<>());
        // Simula o banco atribuindo o ID 42 na primeira gravação.
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocacao -> {
            Pedido p = invocacao.getArgument(0);
            if (p.getId() == null) {
                p.setId(42L);
            }
            return p;
        });

        Pedido salvo = pedidoService.criarPedido(pedido("simples", 1, bloco(1, 1, lamina(0, 0))));

        assertThat(salvo.getId()).isEqualTo(42L);
        assertThat(salvo.getOrderProduction()).isEqualTo(42);
        // Uma gravação para gerar o ID e outra para vincular o orderProduction.
        verify(pedidoRepository, times(2)).save(any(Pedido.class));
    }

    @Test
    @DisplayName("criarPedido recusa número de pedido já usado (409 Conflict)")
    void criarPedidoRecusaNumeroDuplicado() {
        Pedido existente = new Pedido();
        existente.setId(7L);
        existente.setNumeroPedido("1000");
        when(pedidoRepository.findByNumeroPedido("1000"))
                .thenReturn(java.util.Optional.of(existente));

        PedidoDTO novo = pedido("simples", 1, bloco(1, 1));
        novo.setNumeroPedido("1000");

        assertThatThrownBy(() -> pedidoService.criarPedido(novo))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus())
                        .isEqualTo(HttpStatus.CONFLICT));
    }
}
