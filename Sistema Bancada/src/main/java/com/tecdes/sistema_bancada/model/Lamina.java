package com.tecdes.sistema_bancada.model;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/**
 * Entidade que representa uma lâmina dentro de um bloco.
 *
 * Cada bloco pode possuir três lâminas. A lâmina armazena a cor e o padrão
 * escolhido pelo usuário na loja, informações que depois são enviadas ao CLP.
 */
@Entity
public class Lamina {

    /** Identificador único da lâmina no banco de dados. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Código da cor da lâmina. */
    private int cor;

    /** Código do padrão/desenho da lâmina. */
    private int padrao;

    /** Bloco ao qual esta lâmina pertence. */
    @ManyToOne
    @JoinColumn(name = "bloco_id")
    @JsonBackReference
    private Bloco bloco;

    // Métodos de acesso usados pelo JPA e pelos controllers.
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getCor() {
        return cor;
    }

    public void setCor(int cor) {
        this.cor = cor;
    }

    public int getPadrao() {
        return padrao;
    }

    public void setPadrao(int padrao) {
        this.padrao = padrao;
    }

    public Bloco getBloco() {
        return bloco;
    }

    public void setBloco(Bloco bloco) {
        this.bloco = bloco;
    }
}
