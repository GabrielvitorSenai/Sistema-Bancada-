package com.tecdes.sistema_bancada.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tecdes.sistema_bancada.model.Estoque;

public interface EstoqueRepository extends JpaRepository<Estoque, Long> {
    Optional<Estoque> findByPosicaoEstoque(int posicaoEstoque);
    Optional<Estoque> findFirstByCorOrderByPosicaoEstoqueAsc(int cor);
    List<Estoque> findByCorOrderByPosicaoEstoqueAsc(int cor);
    
}

