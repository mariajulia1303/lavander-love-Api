package com.floricultura.api.web;

import org.springframework.core.Ordered;

// ordem em que os filtros rodam em cada requisicao:
//  1. CORS          -> responde o preflight e coloca os headers Access-Control-* (ate nas respostas de erro)
//  2. Versao        -> valida o X-API-Version
//  3. Rate limit    -> conta a requisicao e devolve 429 se passou do limite
//  4. Chave de API  -> valida o X-API-Key e o nivel de acesso (401/403)
//  5. Idempotencia  -> X-Idempotency-Key nos POST
public final class FilterOrder {
    public static final int CORS = Ordered.HIGHEST_PRECEDENCE;
    public static final int VERSAO = Ordered.HIGHEST_PRECEDENCE + 10;
    public static final int RATE_LIMIT = Ordered.HIGHEST_PRECEDENCE + 20;
    public static final int API_KEY = Ordered.HIGHEST_PRECEDENCE + 30;
    public static final int IDEMPOTENCIA = Ordered.HIGHEST_PRECEDENCE + 40;

    private FilterOrder() {
    }
}
