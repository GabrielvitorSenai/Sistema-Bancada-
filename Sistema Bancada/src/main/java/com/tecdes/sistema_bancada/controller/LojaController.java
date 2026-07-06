package com.tecdes.sistema_bancada.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import com.tecdes.sistema_bancada.dto.BlocoDTO;
import com.tecdes.sistema_bancada.dto.LaminaDTO;
import com.tecdes.sistema_bancada.dto.PedidoDTO;
import com.tecdes.sistema_bancada.model.Bloco;
import com.tecdes.sistema_bancada.model.Lamina;
import com.tecdes.sistema_bancada.model.Pedido;
import com.tecdes.sistema_bancada.repository.PedidoRepository;

@Controller
public class LojaController {

    @Autowired
    private PedidoRepository pedidoRepository;

    /**
     * Converte a lista de blocos do DTO em entidades Bloco/Lamina.
     */
    private List<Bloco> montarBlocos(List<BlocoDTO> blocosDTO) {
        List<Bloco> blocos = new ArrayList<>();

        for (BlocoDTO blocoDTO : blocosDTO) {
            Bloco bloco = new Bloco();
            bloco.setCor(blocoDTO.getCorBloco());

            List<Lamina> laminas = new ArrayList<>();
            for (LaminaDTO laminaDTO : blocoDTO.getLaminas()) {
                Lamina lamina = new Lamina();
                lamina.setCor(laminaDTO.getCor());
                lamina.setPadrao(laminaDTO.getPadrao());
                lamina.setBloco(bloco);
                laminas.add(lamina);
            }

            bloco.setLaminas(laminas);
            blocos.add(bloco);
        }

        return blocos;
    }

    /**
     * Substitui os blocos de um pedido JÁ PERSISTIDO.
     *
     * Importante: não se pode trocar a referência da coleção mapeada
     * (pedido.setBlocos(novaLista)) em uma entidade gerenciada pelo Hibernate
     * quando ela usa orphanRemoval — isso quebra o rastreamento da coleção
     * original e o Hibernate lança "A collection with cascade=all-delete-orphan
     * was no longer referenced" ao tentar salvar (o erro 500 do PUT). A forma
     * segura é limpar a coleção existente e adicionar os novos itens nela.
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
     * Garante que o número do pedido informado não pertença a outro pedido.
     * Números em branco não são validados (o campo é opcional).
     *
     * @param idAtual id do pedido sendo editado, ou null durante a criação.
     */
    private boolean numeroPedidoEmUsoPorOutroPedido(String numeroPedido, Long idAtual) {
        if (numeroPedido == null || numeroPedido.isBlank()) {
            return false;
        }
        return pedidoRepository.findByNumeroPedido(numeroPedido)
                .map(Pedido::getId)
                .map(idEncontrado -> !idEncontrado.equals(idAtual))
                .orElse(false);
    }

    /** Log padronizado exibido no terminal para cada ação mapeada. */
    private void logAcao(String acao, Pedido pedido) {
        String numero = (pedido.getNumeroPedido() == null || pedido.getNumeroPedido().isBlank())
                ? "(não informado)"
                : pedido.getNumeroPedido();

        System.out.println();
        System.out.println("┌─ " + acao);
        System.out.println("│  ID: " + pedido.getId());
        System.out.println("│  Número do Pedido: " + numero);
        System.out.println("│  Tipo: " + pedido.getTipo());
        System.out.println("│  Tampa: " + pedido.getTampa());
        System.out.println("│  Status: " + pedido.getStatusOrderProduction());
        System.out.println("│  Blocos: " + (pedido.getBlocos() == null ? 0 : pedido.getBlocos().size()));
        System.out.println("└─────────────────────────────────────────");
    }

    private void logAcao(String acao) {
        System.out.println();
        System.out.println("┌─ " + acao);
        System.out.println("└─────────────────────────────────────────");
    }

    @PostMapping("/salvar-pedidos")
    @ResponseBody
    public ResponseEntity<?> receberPedido(@RequestBody PedidoDTO pedidoDTO) {
        if (numeroPedidoEmUsoPorOutroPedido(pedidoDTO.getNumeroPedido(), null)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Já existe um pedido cadastrado com o número " + pedidoDTO.getNumeroPedido() + ".");
        }

        Pedido pedido = new Pedido();
        pedido.setNumeroPedido(pedidoDTO.getNumeroPedido());
        pedido.setTipo(pedidoDTO.getTipo());
        pedido.setTampa(pedidoDTO.getTampa());
        pedido.setStatusOrderProduction(pedidoDTO.getStatusOrderProduction());
        pedido.setTimeStamp(pedidoDTO.getTimeStamp());
        pedido.setBlocos(montarBlocos(pedidoDTO.getBlocos()));

        // Primeira gravação para gerar o identificador
        pedido = pedidoRepository.save(pedido);

        // Segunda etapa: vincula o código de produção ao ID
        pedido.setOrderProduction(pedido.getId().intValue());

        // Atualiza o registro com o código final
        pedido = pedidoRepository.save(pedido);

        logAcao("📥 POST /salvar-pedidos — pedido criado", pedido);

        return ResponseEntity.ok(pedido.getId());
    }

    @GetMapping("/listar-pedidos")
    @ResponseBody
    public List<Pedido> listarPedidos() {
        List<Pedido> pedidos = pedidoRepository.findAll();
        logAcao("📋 GET /listar-pedidos — " + pedidos.size() + " pedido(s) encontrado(s)");
        return pedidos;
    }

    @DeleteMapping("/api/pedidos/{id}")
    @ResponseBody
    public ResponseEntity<String> excluirPedido(@PathVariable Long id) {
        Optional<Pedido> pedidoOptional = pedidoRepository.findById(id);
        if (pedidoOptional.isEmpty()) {
            logAcao("🗑️ DELETE /api/pedidos/" + id + " — pedido não encontrado");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("NAO ENCONTRADO");
        }

        Pedido pedido = pedidoOptional.get();
        if (!"pendente".equalsIgnoreCase(pedido.getStatusOrderProduction())) {
            logAcao("🗑️ DELETE /api/pedidos/" + id + " — recusado (status " + pedido.getStatusOrderProduction() + ")");
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Somente pedidos pendentes (ainda não iniciados) podem ser excluídos.");
        }

        logAcao("🗑️ DELETE /api/pedidos/" + id + " — pedido excluído", pedido);
        pedidoRepository.deleteById(id);
        return ResponseEntity.ok("DELETADO");
    }

    @PutMapping("/api/pedidos/{id}")
    @ResponseBody
    public ResponseEntity<String> editarPedido(@PathVariable Long id, @RequestBody PedidoDTO pedidoDTO) {
        Optional<Pedido> pedidoOptional = pedidoRepository.findById(id);
        if (pedidoOptional.isEmpty()) {
            logAcao("✏️ PUT /api/pedidos/" + id + " — pedido não encontrado");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Pedido não encontrado.");
        }

        Pedido pedido = pedidoOptional.get();
        if (!"pendente".equalsIgnoreCase(pedido.getStatusOrderProduction())) {
            logAcao("✏️ PUT /api/pedidos/" + id + " — recusado (status " + pedido.getStatusOrderProduction() + ")");
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Somente pedidos pendentes (ainda não iniciados) podem ser editados.");
        }

        if (numeroPedidoEmUsoPorOutroPedido(pedidoDTO.getNumeroPedido(), id)) {
            logAcao("✏️ PUT /api/pedidos/" + id + " — recusado (número duplicado)");
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Já existe um pedido cadastrado com o número " + pedidoDTO.getNumeroPedido() + ".");
        }

        pedido.setNumeroPedido(pedidoDTO.getNumeroPedido());
        pedido.setTipo(pedidoDTO.getTipo());
        pedido.setTampa(pedidoDTO.getTampa());
        substituirBlocos(pedido, pedidoDTO.getBlocos());

        pedido = pedidoRepository.save(pedido);
        logAcao("✏️ PUT /api/pedidos/" + id + " — pedido atualizado", pedido);

        return ResponseEntity.ok("ATUALIZADO");
    }

    @GetMapping("/listar-pedido/{id}")
    @ResponseBody
    public ResponseEntity<Pedido> buscarPedidoPorId(@PathVariable Long id) {
        Optional<Pedido> pedidoOptional = pedidoRepository.findById(id);
        if (pedidoOptional.isEmpty()) {
            logAcao("🔍 GET /listar-pedido/" + id + " — pedido não encontrado");
            return ResponseEntity.notFound().build();
        }

        logAcao("🔍 GET /listar-pedido/" + id + " — pedido consultado", pedidoOptional.get());
        return ResponseEntity.ok(pedidoOptional.get());
    }

    @PutMapping("/salvar-pedido/status")
    @ResponseBody
    public ResponseEntity<String> atualizarStatusPedido(@RequestBody PedidoDTO dto) {
        if (dto.getId() == null) {
            return ResponseEntity.badRequest().body("ID do pedido não informado.");
        }

        Optional<Pedido> pedidoOptional = pedidoRepository.findById(dto.getId());

        if (pedidoOptional.isPresent()) {
            Pedido pedido = pedidoOptional.get();
            pedido.setStatusOrderProduction(dto.getStatusOrderProduction());
            pedido = pedidoRepository.save(pedido);
            logAcao("🔄 PUT /salvar-pedido/status — status atualizado", pedido);
            return ResponseEntity.ok("Status atualizado com sucesso.");
        } else {
            logAcao("🔄 PUT /salvar-pedido/status — pedido " + dto.getId() + " não encontrado");
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Pedido com ID " + dto.getId() + " não encontrado.");
        }
    }

}
