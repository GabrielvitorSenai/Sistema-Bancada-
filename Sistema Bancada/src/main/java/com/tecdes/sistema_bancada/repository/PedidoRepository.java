package com.tecdes.sistema_bancada.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.tecdes.sistema_bancada.model.Pedido;

/**
 * Repositório responsável pelas operações de banco da entidade Pedido.
 *
 * Ao estender JpaRepository, o Spring Data já fornece métodos prontos como
 * save, findById, findAll e deleteById.
 */
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    /**
     * Busca a maior ordem de produção já gravada.
     *
     * COALESCE evita retorno nulo quando ainda não existe nenhum pedido.
     */
    @Query("SELECT COALESCE(MAX(p.orderProduction), 0) FROM Pedido p")
    int findMaxOrderProduction();

    /**
     * Busca o pedido mais recente de um tipo e status específico.
     *
     * Exemplo de uso: pegar o último pedido simples ainda pendente.
     */
    Optional<Pedido> findTopByTipoAndStatusOrderProductionOrderByTimeStampDesc(String tipo, String statusOrderProduction);
}
