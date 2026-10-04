package com.floricultura.api.exception;

// jogada quando algo entra em conflito com o estado atual: nome duplicado,
// ou um id referenciado no POST que nao existe -> vira 409
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
