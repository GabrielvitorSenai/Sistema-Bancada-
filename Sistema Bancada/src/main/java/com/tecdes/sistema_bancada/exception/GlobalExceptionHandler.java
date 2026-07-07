package com.tecdes.sistema_bancada.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Converte as exceções de regra de negócio ({@link BusinessException}) em uma
 * resposta HTTP com o status apropriado e a mensagem em texto puro.
 *
 * O frontend lê a resposta com {@code response.text()} e exibe a mensagem
 * diretamente ao operador, então o corpo é a própria descrição do erro.
 * Só trata {@link BusinessException} — qualquer outra exceção segue o
 * tratamento padrão do Spring, sem alterar o comportamento existente.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<String> handleBusinessException(BusinessException ex) {
        System.out.println();
        System.out.println("⚠️  Regra de negócio [" + ex.getStatus().value() + "]: " + ex.getMessage());
        return ResponseEntity.status(ex.getStatus()).body(ex.getMessage());
    }
}
