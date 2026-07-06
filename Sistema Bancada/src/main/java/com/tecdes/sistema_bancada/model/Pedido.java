package com.tecdes.sistema_bancada.model;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

/**
 * Entidade que representa um pedido criado na Loja.
 *
 * Um pedido possui tipo, cor da tampa, número de OP, status de produção
 * e uma lista de blocos. Essa classe é mapeada pelo JPA para uma tabela
 * no banco de dados MySQL.
 */
@Entity
public class Pedido {

    /** Identificador único do pedido, gerado automaticamente pelo banco. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Número do pedido informado manualmente na Loja para identificação. */
    private String numeroPedido;

    /** Tipo do pedido: simples, duplo ou triplo. */
    private String tipo;

    /** Código da cor da tampa enviada ao seletor de tampas. */
    private int tampa;

    /** Número de ordem de produção utilizado no processo da bancada. */
    private int orderProduction;

    /** Status do pedido dentro da produção, por exemplo: pendente ou concluido. */
    private String statusOrderProduction;

    /** Data e hora em que o registro foi criado. */
    @Column(columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Timestamp timeStamp;

    /**
     * Lista de blocos que formam o pedido.
     *
     * mappedBy indica que a relação é controlada pelo atributo pedido da classe Bloco.
     * CascadeType.ALL faz com que salvar/excluir Pedido também salve/exclua os blocos.
     * orphanRemoval remove blocos órfãos quando saem da lista.
     */
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<Bloco> blocos;

    /**
     * Construtor padrão exigido pelo JPA.
     * Também inicializa a lista para evitar NullPointerException ao adicionar blocos.
     */
    public Pedido() {
        this.blocos = new ArrayList<>();
    }

    // Métodos de acesso usados pelo Spring/JPA e pelos controllers.
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumeroPedido() {
        return numeroPedido;
    }

    public void setNumeroPedido(String numeroPedido) {
        this.numeroPedido = numeroPedido;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public int getTampa() {
        return tampa;
    }

    public void setTampa(int tampa) {
        this.tampa = tampa;
    }

    public int getOrderProduction() {
        return orderProduction;
    }

    public void setOrderProduction(int orderProduction) {
        this.orderProduction = orderProduction;
    }

    public String getStatusOrderProduction() {
        return statusOrderProduction;
    }

    public void setStatusOrderProduction(String statusOrderProduction) {
        this.statusOrderProduction = statusOrderProduction;
    }

    public Timestamp getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(Timestamp timeStamp) {
        this.timeStamp = timeStamp;
    }

    public List<Bloco> getBlocos() {
        return blocos;
    }

    /**
     * Define os blocos do pedido e garante o vínculo reverso.
     *
     * Isso é importante porque cada Bloco precisa saber a qual Pedido pertence
     * antes de ser salvo pelo JPA.
     */
    public void setBlocos(List<Bloco> blocos) {
        this.blocos = blocos;
        if (blocos != null) {
            for (Bloco bloco : blocos) {
                bloco.setPedido(this);
            }
        }
    }
}
