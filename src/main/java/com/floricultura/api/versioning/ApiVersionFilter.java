package com.floricultura.api.versioning;

import java.io.IOException;
import java.util.Set;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.floricultura.api.web.ApiPaths;
import com.floricultura.api.web.ErrorResponseWriter;
import com.floricultura.api.web.FilterOrder;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// versionamento por header: X-API-Version.
//  - sem header -> versao 1 (padrao, pra nao quebrar quem ja usa a API)
//  - "1" ou "2" -> ok
//  - qualquer outra coisa -> 400 avisando as versoes suportadas
// a resposta sempre volta com X-API-Version dizendo qual versao respondeu.
@Component
@Order(FilterOrder.VERSAO)
public class ApiVersionFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-API-Version";
    public static final String VERSAO_PADRAO = "1";
    public static final Set<String> SUPORTADAS = Set.of("1", "2");

    private final ErrorResponseWriter errorWriter;

    public ApiVersionFilter(ErrorResponseWriter errorWriter) {
        this.errorWriter = errorWriter;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return "OPTIONS".equalsIgnoreCase(request.getMethod()) || !ApiPaths.ehApi(ApiPaths.caminho(request));
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain chain) throws ServletException, IOException {
        String versao = request.getHeader(HEADER);
        if (versao == null || versao.isBlank()) {
            versao = VERSAO_PADRAO;
        }
        if (!SUPORTADAS.contains(versao)) {
            errorWriter.write(response, HttpStatus.BAD_REQUEST,
                    "Versao da API '" + versao + "' nao suportada. Use o header X-API-Version com 1 ou 2.");
            return;
        }
        response.setHeader(HEADER, versao);
        chain.doFilter(request, response);
    }
}
