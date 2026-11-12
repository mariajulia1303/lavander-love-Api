package com.floricultura.api.web;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

// lista central dos caminhos que sao "da API" (e passam pelos filtros de seguranca,
// rate limit, idempotencia e versao). swagger, h2-console e arquivos estaticos ficam de fora.
public final class ApiPaths {

    public static final List<String> RECURSOS = List.of(
            "/clientes", "/enderecos", "/produtos", "/categorias", "/pedidos", "/api-keys", "/limites", "/auth");

    // catalogo publico (e a politica de rate limit): qualquer um pode consultar (GET) sem chave
    public static final List<String> PUBLICOS_GET = List.of("/produtos", "/categorias", "/limites");

    // area da conta (cadastro/login/chave pessoal): nao pede X-API-Key nem X-Idempotency-Key
    public static final List<String> CONTA = List.of("/auth");

    private ApiPaths() {
    }

    public static String caminho(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String ctx = request.getContextPath();
        return (ctx != null && !ctx.isEmpty() && uri.startsWith(ctx)) ? uri.substring(ctx.length()) : uri;
    }

    public static boolean ehApi(String caminho) {
        return comecaCom(caminho, RECURSOS);
    }

    public static boolean comecaCom(String caminho, List<String> prefixos) {
        for (String p : prefixos) {
            if (caminho.equals(p) || caminho.startsWith(p + "/")) {
                return true;
            }
        }
        return false;
    }
}
