package com.tecdes.sistema_bancada.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * Entidade que representa uma posição do Magazine de Estoque.
 *
 * A bancada possui 28 posições de estoque. Cada posição pode estar vazia
 * ou conter um bloco de uma determinada cor.
 */
@Entity
public class Estoque {

    /** Identificador único do registro no banco. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Número da posição física do magazine de estoque, de 1 até 28. */
    private int posicaoEstoque;

    /** Código da cor do bloco naquela posição. Exemplo: 0 vazio, 1 preto, 2 vermelho, 3 azul. */
    private int cor;

    // Métodos de acesso usados pelo JPA e pelos controllers.
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getPosicaoEstoque() {
        return posicaoEstoque;
    }

    public void setPosicaoEstoque(int posicaoEstoque) {
        this.posicaoEstoque = posicaoEstoque;
    }

    public int getCor() {
        return cor;
    }

    public void setCor(int cor) {
        this.cor = cor;
    }
}
