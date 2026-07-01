package com.tecdes.sistema_bancada.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.tecdes.sistema_bancada.model.Expedicao;

/**
 * Repositório responsável pelas operações de banco da expedição.
 *
 * A expedição guarda quais pedidos já foram concluídos e em qual posição
 * física do magazine de expedição eles estão armazenados.
 */
public interface ExpedicaoRepository extends JpaRepository<Expedicao, Long> {

    /** Busca uma posição de expedição pelo número do pedido/OP. */
    Optional<Expedicao> findByOrderNumber(int orderNumber);

    /** Busca uma posição específica da expedição, de 1 até 12. */
    Optional<Expedicao> findByPosicaoExpedicao(int posicaoExpedicao);

    /** Busca a primeira posição onde uma OP aparece, útil para detectar duplicidade. */
    Optional<Expedicao> findFirstByOrderNumberEqualsOrderByPosicaoExpedicaoAsc(int orderNumber);

    /** Retorna apenas os números das posições ocupadas na expedição. */
    @Query("SELECT e.posicaoExpedicao FROM Expedicao e")
    List<Integer> findAllPosicoesOcupadas();
}
