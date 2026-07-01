package com.tecdes.sistema_bancada.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * Entidade que representa uma posição do Magazine de Expedição.
 *
 * A expedição possui 12 posições. Cada posição pode guardar o número/OP
 * de um pedido já concluído pela linha didática.
 */
@Entity
public class Expedicao {

    /** Identificador único do registro no banco. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Número da posição física da expedição, de 1 até 12. */
    private int posicaoExpedicao;

    /** Número do pedido/ordem de produção guardado naquela posição. */
    private int orderNumber;

    // Métodos de acesso usados pelo JPA e pelos controllers.
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getPosicaoExpedicao() {
        return posicaoExpedicao;
    }

    public void setPosicaoExpedicao(int posicaoExpedicao) {
        this.posicaoExpedicao = posicaoExpedicao;
    }

    public int getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(int orderNumber) {
        this.orderNumber = orderNumber;
    }
}
