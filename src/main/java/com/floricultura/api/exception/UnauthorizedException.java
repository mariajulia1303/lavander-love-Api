package com.floricultura.api.exception;

// login errado ou sessao ausente/expirada -> 401
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
