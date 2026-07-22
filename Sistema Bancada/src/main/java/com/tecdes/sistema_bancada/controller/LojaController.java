package com.tecdes.sistema_bancada.controller;

import java.util.List;

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

import com.tecdes.sistema_bancada.dto.PedidoDTO;
import com.tecdes.sistema_bancada.model.Pedido;
import com.tecdes.sistema_bancada.service.PedidoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Endpoints de gestão de pedidos da Loja.
 *
 * Toda a lógica de negócio (validações das Regras de Ouro e persistência) fica
 * na camada de Service ({@link PedidoService}); este controller só expõe os
 * endpoints, delega e registra a ação no terminal. Mantém os caminhos originais
 * usados pelo frontend e adiciona os caminhos RESTful pedidos na Situação de
 * Aprendizagem (/api/pedidos, /api/pedidos/{id}/status).
 */
@Controller
@Tag(name = "Pedidos", description = "Criação, consulta, edição e mudança de status dos pedidos da Loja.")
public class LojaController {

    @Autowired
    private PedidoService pedidoService;

    // ---------------------------------------------------------------------
    // Criação
    // ---------------------------------------------------------------------

    /** Caminho original usado pela tela da Loja. Retorna o ID gerado. */
    @PostMapping("/salvar-pedidos")
    @ResponseBody
    public ResponseEntity<Long> receberPedido(@RequestBody PedidoDTO pedidoDTO) {
        Pedido pedido = pedidoService.criarPedido(pedidoDTO);
        logAcao("POST /salvar-pedidos — pedido criado", pedido);
        return ResponseEntity.ok(pedido.getId());
    }

    /** Caminho RESTful pedido no enunciado. Retorna 201 com o pedido criado. */
    @Operation(summary = "Cria um novo pedido",
            description = "Valida as Regras de Ouro (tipo x quantidade de blocos, cor da tampa 1-3, "
                    + "no máximo 3 lâminas por bloco e disponibilidade de estoque) antes de gravar.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pedido criado com sucesso"),
            @ApiResponse(responseCode = "409", description = "Número de pedido já cadastrado"),
            @ApiResponse(responseCode = "422", description = "Violação de regra de negócio (ex.: estoque insuficiente, tampa inválida)")
    })
    @PostMapping("/api/pedidos")
    @ResponseBody
    public ResponseEntity<Pedido> criarPedidoRest(@RequestBody PedidoDTO pedidoDTO) {
        Pedido pedido = pedidoService.criarPedido(pedidoDTO);
        logAcao("POST /api/pedidos — pedido criado", pedido);
        return ResponseEntity.status(HttpStatus.CREATED).body(pedido);
    }

    // ---------------------------------------------------------------------
    // Listagem / consulta
    // ---------------------------------------------------------------------

    @GetMapping("/listar-pedidos")
    @ResponseBody
    public List<Pedido> listarPedidos() {
        List<Pedido> pedidos = pedidoService.listar();
        logAcao("GET /listar-pedidos — " + pedidos.size() + " pedido(s) encontrado(s)");
        return pedidos;
    }

    /** Caminho RESTful equivalente a /listar-pedidos. */
    @Operation(summary = "Lista todos os pedidos",
            description = "Retorna todos os pedidos cadastrados, com seus blocos e lâminas.")
    @GetMapping("/api/pedidos")
    @ResponseBody
    public List<Pedido> listarPedidosRest() {
        List<Pedido> pedidos = pedidoService.listar();
        logAcao("GET /api/pedidos — " + pedidos.size() + " pedido(s) encontrado(s)");
        return pedidos;
    }

    @GetMapping("/listar-pedido/{id}")
    @ResponseBody
    public ResponseEntity<Pedido> buscarPedidoPorId(@PathVariable Long id) {
        return pedidoService.buscarPorId(id)
                .map(pedido -> {
                    logAcao("GET /listar-pedido/" + id + " — pedido consultado", pedido);
                    return ResponseEntity.ok(pedido);
                })
                .orElseGet(() -> {
                    logAcao("GET /listar-pedido/" + id + " — pedido não encontrado");
                    return ResponseEntity.notFound().build();
                });
    }

    // ---------------------------------------------------------------------
    // Edição
    // ---------------------------------------------------------------------

    @PutMapping("/api/pedidos/{id}")
    @ResponseBody
    public ResponseEntity<String> editarPedido(@PathVariable Long id, @RequestBody PedidoDTO pedidoDTO) {
        Pedido pedido = pedidoService.editarPedido(id, pedidoDTO);
        logAcao("PUT /api/pedidos/" + id + " — pedido atualizado", pedido);
        return ResponseEntity.ok("ATUALIZADO");
    }

    // ---------------------------------------------------------------------
    // Exclusão
    // ---------------------------------------------------------------------

    @DeleteMapping("/api/pedidos/{id}")
    @ResponseBody
    public ResponseEntity<String> excluirPedido(@PathVariable Long id) {
        pedidoService.excluirPedido(id);
        logAcao("DELETE /api/pedidos/" + id + " — pedido excluído");
        return ResponseEntity.ok("DELETADO");
    }

    // ---------------------------------------------------------------------
    // Atualização de status
    // ---------------------------------------------------------------------

    /** Caminho original usado internamente (recebe o ID no corpo). */
    @PutMapping("/salvar-pedido/status")
    @ResponseBody
    public ResponseEntity<String> atualizarStatusPedido(@RequestBody PedidoDTO dto) {
        if (dto.getId() == null) {
            return ResponseEntity.badRequest().body("ID do pedido não informado.");
        }
        Pedido pedido = pedidoService.atualizarStatus(dto.getId(), dto.getStatusOrderProduction());
        logAcao("PUT /salvar-pedido/status — status atualizado", pedido);
        return ResponseEntity.ok("Status atualizado com sucesso.");
    }

    /** Caminho RESTful pedido no enunciado (ID na URL). */
    @Operation(summary = "Atualiza o status de produção do pedido",
            description = "Aceita o texto (pendente/producao/concluido) ou o código numérico "
                    + "(1 = pendente, 2 = produção, 3 = concluído). Reflete a mudança de etapa da linha.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status atualizado"),
            @ApiResponse(responseCode = "404", description = "Pedido não encontrado"),
            @ApiResponse(responseCode = "422", description = "Status inválido")
    })
    @PutMapping("/api/pedidos/{id}/status")
    @ResponseBody
    public ResponseEntity<String> atualizarStatusRest(@PathVariable Long id, @RequestBody PedidoDTO dto) {
        Pedido pedido = pedidoService.atualizarStatus(id, dto.getStatusOrderProduction());
        logAcao("PUT /api/pedidos/" + id + "/status — status atualizado", pedido);
        return ResponseEntity.ok("Status atualizado com sucesso.");
    }

    // ---------------------------------------------------------------------
    // Log padronizado no terminal
    // ---------------------------------------------------------------------

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
}
