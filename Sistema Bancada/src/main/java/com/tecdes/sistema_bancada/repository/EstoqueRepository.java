package com.tecdes.sistema_bancada.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tecdes.sistema_bancada.model.Estoque;

/**
 * Repositório responsável por consultar e atualizar o magazine de estoque.
 *
 * O Spring Data JPA monta as consultas automaticamente a partir do nome dos métodos.
 */
public interface EstoqueRepository extends JpaRepository<Estoque, Long> {

    /** Busca uma posição específica do estoque, de 1 até 28. */
    Optional<Estoque> findByPosicaoEstoque(int posicaoEstoque);

    /** Busca a primeira posição que tenha a cor informada, em ordem crescente. */
    Optional<Estoque> findFirstByCorOrderByPosicaoEstoqueAsc(int cor);

    /** Lista todas as posições de uma cor específica, ordenadas pela posição física. */
    List<Estoque> findByCorOrderByPosicaoEstoqueAsc(int cor);
}
