package com.tecdes.sistema_bancada.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

/**
 * Entidade que representa um bloco/andar do pedido.
 *
 * Um pedido pode ser simples, duplo ou triplo. Cada andar é representado por um
 * Bloco e cada Bloco possui até três Lâminas associadas.
 */
@Entity
public class Bloco {

    /** Identificador único do bloco no banco de dados. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Código da cor do bloco físico usado na montagem. */
    private int cor;

    /** Pedido ao qual este bloco pertence. */
    @ManyToOne
    @JoinColumn(name = "pedido_id")
    @JsonBackReference
    private Pedido pedido;

    /**
     * Lâminas associadas ao bloco.
     *
     * CascadeType.ALL salva/exclui as lâminas junto com o bloco.
     * orphanRemoval remove lâminas que forem retiradas da lista.
     */
    @OneToMany(mappedBy = "bloco", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<Lamina> laminas;

    /** Construtor padrão usado pelo JPA e para iniciar a lista de lâminas. */
    public Bloco() {
        this.laminas = new ArrayList<>();
    }

    // Métodos de acesso usados pelo JPA, controllers e serialização JSON.
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

    public Pedido getPedido() {
        return pedido;
    }

    public void setPedido(Pedido pedido) {
        this.pedido = pedido;
    }

    public List<Lamina> getLaminas() {
        return laminas;
    }

    /**
     * Define as lâminas do bloco e garante o vínculo reverso.
     *
     * Cada Lamina precisa apontar para este Bloco para que o relacionamento seja
     * salvo corretamente no banco pelo JPA.
     */
    public void setLaminas(List<Lamina> laminas) {
        this.laminas = laminas;
        if (laminas != null) {
            for (Lamina lamina : laminas) {
                lamina.setBloco(this);
            }
        }
    }
}
