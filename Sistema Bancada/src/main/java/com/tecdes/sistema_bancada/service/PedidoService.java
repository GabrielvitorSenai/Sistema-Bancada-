package com.tecdes.sistema_bancada.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.tecdes.sistema_bancada.dto.BlocoDTO;
import com.tecdes.sistema_bancada.dto.LaminaDTO;
import com.tecdes.sistema_bancada.dto.PedidoDTO;
import com.tecdes.sistema_bancada.exception.BusinessException;
import com.tecdes.sistema_bancada.model.Bloco;
import com.tecdes.sistema_bancada.model.Estoque;
import com.tecdes.sistema_bancada.model.Lamina;
import com.tecdes.sistema_bancada.model.Pedido;
import com.tecdes.sistema_bancada.repository.EstoqueRepository;
import com.tecdes.sistema_bancada.repository.PedidoRepository;

/**
 * Camada de negócio dos pedidos da planta SMART 4.0.
 *
 * Concentra as "Regras de Ouro" da produção (validações de tipo, composição,
 * lâminas, cor da tampa e disponibilidade de estoque) antes de qualquer
 * gravação, funcionando como o filtro de qualidade descrito na Situação de
 * Aprendizagem: um pedido que viole as regras é recusado com um erro claro,
 * em vez de ser gravado no banco.
 *
 * Os controllers apenas delegam para cá; toda a lógica de validação e
 * persistência de Pedido/Bloco/Lâmina vive nesta classe.
 */
@Service
public class PedidoService {

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private EstoqueRepository estoqueRepository;

    // ---------------------------------------------------------------------
    // Consultas
    // ---------------------------------------------------------------------

    public List<Pedido> listar() {
        return pedidoRepository.findAll();
    }

    public Optional<Pedido> buscarPorId(Long id) {
        return pedidoRepository.findById(id);
    }

    // ---------------------------------------------------------------------
    // Criação
    // ---------------------------------------------------------------------

    /**
     * Cria um novo pedido depois de validar todas as regras de negócio.
     * Vincula o código de produção ao ID gerado, como no fluxo original.
     */
    public Pedido criarPedido(PedidoDTO dto) {
        validarNumeroPedidoUnico(dto.getNumeroPedido(), null);
        validarComposicao(dto);

        Pedido pedido = new Pedido();
        pedido.setNumeroPedido(dto.getNumeroPedido());
        pedido.setTipo(dto.getTipo());
        pedido.setTampa(dto.getTampa());
        pedido.setStatusOrderProduction(
                dto.getStatusOrderProduction() == null ? "pendente" : dto.getStatusOrderProduction());
        pedido.setTimeStamp(dto.getTimeStamp());
        pedido.setBlocos(montarBlocos(dto.getBlocos()));

        // Primeira gravação para gerar o identificador.
        pedido = pedidoRepository.save(pedido);

        // Vincula o código de produção ao ID e regrava.
        pedido.setOrderProduction(pedido.getId().intValue());
        return pedidoRepository.save(pedido);
    }

    // ---------------------------------------------------------------------
    // Edição
    // ---------------------------------------------------------------------

    /**
     * Edita um pedido ainda pendente. Recusa a edição de pedidos que já
     * entraram em produção ou foram concluídos.
     */
    public Pedido editarPedido(Long id, PedidoDTO dto) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Pedido não encontrado."));

        exigirPendente(pedido, "editados");
        validarNumeroPedidoUnico(dto.getNumeroPedido(), id);
        validarComposicao(dto);

        pedido.setNumeroPedido(dto.getNumeroPedido());
        pedido.setTipo(dto.getTipo());
        pedido.setTampa(dto.getTampa());
        substituirBlocos(pedido, dto.getBlocos());

        return pedidoRepository.save(pedido);
    }

    // ---------------------------------------------------------------------
    // Exclusão
    // ---------------------------------------------------------------------

    /** Exclui um pedido ainda pendente (não iniciado). */
    public void excluirPedido(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Pedido não encontrado."));

        exigirPendente(pedido, "excluídos");
        pedidoRepository.deleteById(id);
    }

    // ---------------------------------------------------------------------
    // Atualização de status
    // ---------------------------------------------------------------------

    /**
     * Atualiza o status do pedido. Aceita tanto o texto
     * (pendente/producao/concluido) quanto o código numérico do enunciado
     * (1 - pendente, 2 - produção, 3 - concluído).
     */
    public Pedido atualizarStatus(Long id, String statusOrderProduction) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "Pedido com ID " + id + " não encontrado."));

        pedido.setStatusOrderProduction(normalizarStatus(statusOrderProduction));
        return pedidoRepository.save(pedido);
    }

    // ---------------------------------------------------------------------
    // Regras de Ouro (validações)
    // ---------------------------------------------------------------------

    /**
     * Valida toda a composição do pedido antes de gravar: quantidade de blocos
     * por tipo, cor da tampa, cores/padrões/quantidade de lâminas, cores de
     * bloco e disponibilidade de estoque.
     */
    public void validarComposicao(PedidoDTO dto) {
        List<BlocoDTO> blocos = dto.getBlocos();
        if (blocos == null || blocos.isEmpty()) {
            throw new BusinessException("O pedido deve ter pelo menos um bloco.");
        }

        validarTipoBlocos(dto.getTipo(), blocos.size());
        validarTampa(dto.getTampa());
        validarBlocosELaminas(blocos);
        validarEstoque(blocos);
    }

    /** Regra de tipo: simples = 1 bloco, duplo = 2, triplo = 3. */
    private void validarTipoBlocos(String tipo, int quantidadeBlocos) {
        int esperado = blocosEsperadosPorTipo(tipo);
        if (quantidadeBlocos != esperado) {
            throw new BusinessException("Pedidos do tipo \"" + tipo + "\" exigem exatamente "
                    + esperado + " bloco(s), mas foram enviados " + quantidadeBlocos + ".");
        }
    }

    private int blocosEsperadosPorTipo(String tipo) {
        if (tipo == null) {
            throw new BusinessException("Tipo de pedido não informado.");
        }
        switch (tipo.trim().toLowerCase()) {
            case "simples":
                return 1;
            case "duplo":
                return 2;
            case "triplo":
                return 3;
            default:
                throw new BusinessException("Tipo de pedido inválido: \"" + tipo
                        + "\". Use simples, duplo ou triplo.");
        }
    }

    /** Cor da tampa deve ser 1 (preto), 2 (vermelho) ou 3 (azul). */
    private void validarTampa(int tampa) {
        if (tampa < 1 || tampa > 3) {
            throw new BusinessException("Cor da tampa inválida: " + tampa
                    + ". Use 1 (preto), 2 (vermelho) ou 3 (azul).");
        }
    }

    /**
     * Valida cada bloco e suas lâminas: cor do bloco válida, no máximo 3
     * lâminas por bloco, e cor/padrão de cada lâmina dentro dos códigos aceitos.
     */
    private void validarBlocosELaminas(List<BlocoDTO> blocos) {
        for (BlocoDTO bloco : blocos) {
            int cor = bloco.getCorBloco();
            if (cor < 1 || cor > 3) {
                throw new BusinessException("Cor de bloco inválida: " + cor
                        + ". Use 1 (preto), 2 (vermelho) ou 3 (azul).");
            }

            List<LaminaDTO> laminas = bloco.getLaminas();
            if (laminas == null) {
                continue;
            }
            if (laminas.size() > 3) {
                throw new BusinessException("Cada bloco pode ter no máximo 3 lâminas; o andar "
                        + bloco.getAndar() + " tem " + laminas.size() + ".");
            }
            for (LaminaDTO lamina : laminas) {
                // 0 = nenhuma lâmina naquela posição; 1..6 = cores válidas.
                if (lamina.getCor() < 0 || lamina.getCor() > 6) {
                    throw new BusinessException("Cor de lâmina inválida: " + lamina.getCor()
                            + ". Use de 1 a 6 (ou 0 para nenhuma).");
                }
                if (lamina.getPadrao() < 0 || lamina.getPadrao() > 3) {
                    throw new BusinessException("Padrão de lâmina inválido: " + lamina.getPadrao()
                            + ". Use 0 (nenhum), 1 (casa), 2 (navio) ou 3 (estrela).");
                }
            }
        }
    }

    /**
     * Valida se o estoque configurado tem blocos suficientes das cores pedidas.
     *
     * Segue a mesma lógica do aviso já existente no frontend: conta as cores
     * usadas neste pedido e compara com o disponível por cor. Se o estoque
     * ainda não foi configurado (nenhuma posição cadastrada), não bloqueia.
     */
    private void validarEstoque(List<BlocoDTO> blocos) {
        List<Estoque> posicoes = estoqueRepository.findAll();
        if (posicoes.isEmpty()) {
            return; // Sem estoque configurado: não bloqueia (igual ao frontend).
        }

        Map<Integer, Integer> disponivel = new HashMap<>();
        for (Estoque e : posicoes) {
            int cor = e.getCor();
            if (cor >= 1 && cor <= 3) {
                disponivel.merge(cor, 1, Integer::sum);
            }
        }

        Map<Integer, Integer> usadas = new HashMap<>();
        for (BlocoDTO bloco : blocos) {
            int cor = bloco.getCorBloco();
            if (cor < 1 || cor > 3) {
                cor = 3; // vazio cai em azul, como em codigoCorBloco no frontend.
            }
            usadas.merge(cor, 1, Integer::sum);
        }

        List<String> faltas = new ArrayList<>();
        for (int cor = 1; cor <= 3; cor++) {
            int precisa = usadas.getOrDefault(cor, 0);
            int tem = disponivel.getOrDefault(cor, 0);
            if (precisa > tem) {
                faltas.add(nomeCor(cor) + " (pedido: " + precisa + ", estoque: " + tem + ")");
            }
        }

        if (!faltas.isEmpty()) {
            throw new BusinessException("Estoque insuficiente para: " + String.join("; ", faltas) + ".");
        }
    }

    /** Garante que o número do pedido informado não pertença a outro pedido. */
    private void validarNumeroPedidoUnico(String numeroPedido, Long idAtual) {
        if (numeroPedido == null || numeroPedido.isBlank()) {
            return;
        }
        boolean emUso = pedidoRepository.findByNumeroPedido(numeroPedido)
                .map(Pedido::getId)
                .map(idEncontrado -> !idEncontrado.equals(idAtual))
                .orElse(false);
        if (emUso) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "Já existe um pedido cadastrado com o número " + numeroPedido + ".");
        }
    }

    private void exigirPendente(Pedido pedido, String acao) {
        if (!"pendente".equalsIgnoreCase(pedido.getStatusOrderProduction())) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "Somente pedidos pendentes (ainda não iniciados) podem ser " + acao + ".");
        }
    }

    // ---------------------------------------------------------------------
    // Auxiliares de montagem
    // ---------------------------------------------------------------------

    /** Converte a lista de blocos do DTO em entidades Bloco/Lamina. */
    private List<Bloco> montarBlocos(List<BlocoDTO> blocosDTO) {
        List<Bloco> blocos = new ArrayList<>();
        for (BlocoDTO blocoDTO : blocosDTO) {
            Bloco bloco = new Bloco();
            bloco.setCor(blocoDTO.getCorBloco());

            List<Lamina> laminas = new ArrayList<>();
            if (blocoDTO.getLaminas() != null) {
                for (LaminaDTO laminaDTO : blocoDTO.getLaminas()) {
                    Lamina lamina = new Lamina();
                    lamina.setCor(laminaDTO.getCor());
                    lamina.setPadrao(laminaDTO.getPadrao());
                    lamina.setBloco(bloco);
                    laminas.add(lamina);
                }
            }

            bloco.setLaminas(laminas);
            blocos.add(bloco);
        }
        return blocos;
    }

    /**
     * Substitui os blocos de um pedido JÁ PERSISTIDO reaproveitando a coleção
     * gerenciada (clear + add). Trocar a referência da coleção mapeada com
     * orphanRemoval quebraria o rastreamento do Hibernate e causaria erro no
     * flush.
     */
    private void substituirBlocos(Pedido pedido, List<BlocoDTO> blocosDTO) {
        List<Bloco> novosBlocos = montarBlocos(blocosDTO);
        pedido.getBlocos().clear();
        for (Bloco bloco : novosBlocos) {
            bloco.setPedido(pedido);
            pedido.getBlocos().add(bloco);
        }
    }

    /**
     * Normaliza o status recebido: aceita o código numérico do enunciado
     * (1/2/3) ou o texto já usado no sistema.
     */
    private String normalizarStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new BusinessException("Status do pedido não informado.");
        }
        switch (status.trim().toLowerCase()) {
            case "1":
            case "pendente":
                return "pendente";
            case "2":
            case "producao":
            case "produção":
                return "producao";
            case "3":
            case "concluido":
            case "concluído":
                return "concluido";
            default:
                throw new BusinessException("Status inválido: \"" + status
                        + "\". Use 1 (pendente), 2 (produção) ou 3 (concluído).");
        }
    }

    private String nomeCor(int cor) {
        switch (cor) {
            case 1:
                return "Preto";
            case 2:
                return "Vermelho";
            case 3:
                return "Azul";
            default:
                return "Cor " + cor;
        }
    }
}
