package com.floricultura.api.exception;

// jogada quando o recurso pedido (GET/PUT/DELETE por id) nao existe -> vira 404
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
