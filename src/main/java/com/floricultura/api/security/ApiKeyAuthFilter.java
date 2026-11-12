package com.floricultura.api.security;

import java.io.IOException;
import java.util.Optional;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.floricultura.api.entity.ApiKey;
import com.floricultura.api.entity.NivelAcesso;
import com.floricultura.api.web.ApiPaths;
import com.floricultura.api.web.ErrorResponseWriter;
import com.floricultura.api.web.FilterOrder;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// autenticacao por chave de API (header X-API-Key) + nivel de acesso.
//  401 Unauthorized -> rota protegida sem chave, ou chave invalida/desativada
//  403 Forbidden    -> chave valida, mas o nivel dela nao basta pra essa operacao
// as regras de qual rota exige qual nivel ficam em PoliticaAcesso.
@Component
@Order(FilterOrder.API_KEY)
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private final ApiKeyResolver resolver;
    private final ErrorResponseWriter errorWriter;

    public ApiKeyAuthFilter(ApiKeyResolver resolver, ErrorResponseWriter errorWriter) {
        this.resolver = resolver;
        this.errorWriter = errorWriter;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return "OPTIONS".equalsIgnoreCase(request.getMethod()) || !ApiPaths.ehApi(ApiPaths.caminho(request));
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader(ApiKeyResolver.HEADER);
        boolean enviouChave = header != null && !header.isBlank();
        Optional<ApiKey> chave = resolver.resolver(request);
        NivelAcesso exigido = PoliticaAcesso.nivelExigido(request.getMethod(), ApiPaths.caminho(request));

        // mandou uma chave mas ela nao vale: avisa, mesmo em rota publica (ajuda a achar erro de digitacao)
        if (enviouChave && chave.isEmpty()) {
            negar(response, HttpStatus.UNAUTHORIZED, "Chave de API invalida ou desativada.");
            return;
        }
        if (exigido == null) {
            chain.doFilter(request, response);
            return;
        }
        if (chave.isEmpty()) {
            negar(response, HttpStatus.UNAUTHORIZED,
                    "Esta operacao exige uma chave de API. Envie o header X-API-Key (nivel minimo: " + exigido + ").");
            return;
        }
        NivelAcesso nivel = chave.get().getNivel();
        if (!nivel.permite(exigido)) {
            errorWriter.write(response, HttpStatus.FORBIDDEN,
                    "Sua chave tem nivel " + nivel + ", mas esta operacao exige nivel " + exigido + ".");
            return;
        }
        chain.doFilter(request, response);
    }

    private void negar(HttpServletResponse response, HttpStatus status, String msg) throws IOException {
        response.setHeader("WWW-Authenticate", "ApiKey header=\"" + ApiKeyResolver.HEADER + "\"");
        errorWriter.write(response, status, msg);
    }
}
