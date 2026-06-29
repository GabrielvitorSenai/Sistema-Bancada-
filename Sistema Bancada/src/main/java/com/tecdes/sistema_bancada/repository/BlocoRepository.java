package com.tecdes.sistema_bancada.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tecdes.sistema_bancada.model.Bloco;

public interface BlocoRepository extends JpaRepository<Bloco, Long> {
}