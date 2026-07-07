package com.tecdes.sistema_bancada.exception;

import org.springframework.http.HttpStatus;

/**
 * Exceção usada pela camada de Service quando uma regra de negócio da planta
 * SMART 4.0 é violada (ex.: pedido triplo sem 3 blocos, estoque insuficiente,
 * cor de tampa inexistente).
 *
 * Carrega o HttpStatus que deve ser retornado ao cliente, permitindo diferenciar
 * uma validação de regra (422), um recurso não encontrado (404) e um conflito de
 * estado (409). O {@code GlobalExceptionHandler} converte a exceção na resposta
 * HTTP com a mensagem clara para o operador.
 */
public class BusinessException extends RuntimeException {

    private final HttpStatus status;

    /** Cria uma violação de regra de negócio padrão (422 - Unprocessable Entity). */
    public BusinessException(String message) {
        this(HttpStatus.UNPROCESSABLE_ENTITY, message);
    }

    public BusinessException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
