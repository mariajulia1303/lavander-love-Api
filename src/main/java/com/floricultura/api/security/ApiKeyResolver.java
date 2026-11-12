package com.floricultura.api.security;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.floricultura.api.entity.ApiKey;
import com.floricultura.api.service.ApiKeyService;

import jakarta.servlet.http.HttpServletRequest;

// le o header X-API-Key e valida UMA vez por requisicao (o resultado fica guardado num
// atributo da request pra o rate limit, a autenticacao e os controllers reaproveitarem).
@Component
public class ApiKeyResolver {

    public static final String HEADER = "X-API-Key";
    public static final String ATRIBUTO = "lavander.apiKey";
    private static final String ATRIBUTO_RESOLVIDO = "lavander.apiKey.resolvida";

    private final ApiKeyService apiKeyService;

    public ApiKeyResolver(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    public Optional<ApiKey> resolver(HttpServletRequest request) {
        if (request.getAttribute(ATRIBUTO_RESOLVIDO) == null) {
            request.setAttribute(ATRIBUTO_RESOLVIDO, Boolean.TRUE);
            apiKeyService.validar(request.getHeader(HEADER))
                    .ifPresent(k -> request.setAttribute(ATRIBUTO, k));
        }
        return atual(request);
    }

    // chave autenticada da requisicao atual (vazio se nao veio ou e invalida)
    public static Optional<ApiKey> atual(HttpServletRequest request) {
        return Optional.ofNullable((ApiKey) request.getAttribute(ATRIBUTO));
    }
}
