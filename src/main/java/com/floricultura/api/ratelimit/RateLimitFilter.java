package com.floricultura.api.ratelimit;

import java.io.IOException;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.floricultura.api.entity.ApiKey;
import com.floricultura.api.security.ApiKeyResolver;
import com.floricultura.api.web.ApiPaths;
import com.floricultura.api.web.ErrorResponseWriter;
import com.floricultura.api.web.FilterOrder;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// rate limit com TOKEN BUCKET (ver TokenBucket e RateLimitService).
// roda ANTES de chegar no controller/banco: se o balde estiver vazio a requisicao nem e processada.
//
// toda resposta da API leva:
//  X-RateLimit-Limit     -> tamanho do balde (requisicoes de uma vez)
//  X-RateLimit-Remaining -> fichas que ainda restam
//  X-RateLimit-Reset     -> segundos ate o balde encher de novo
//  X-RateLimit-Policy    -> plano aplicado e taxa de recarga (ex: "GRATUITO;capacidade=8;recarga=20/min")
// balde vazio -> 429 Too Many Requests + Retry-After (segundos ate cair a proxima ficha).
// insistiu depois do 429 -> bloqueio temporario (penalidade), tambem 429 + Retry-After.
@Component
@Order(FilterOrder.RATE_LIMIT)
public class RateLimitFilter extends OncePerRequestFilter {

    private final ApiKeyResolver resolver;
    private final RateLimitService service;
    private final ErrorResponseWriter errorWriter;

    public RateLimitFilter(ApiKeyResolver resolver, RateLimitService service, ErrorResponseWriter errorWriter) {
        this.resolver = resolver;
        this.service = service;
        this.errorWriter = errorWriter;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return "OPTIONS".equalsIgnoreCase(request.getMethod()) || !ApiPaths.ehApi(ApiPaths.caminho(request));
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain chain) throws ServletException, IOException {
        ApiKey chave = resolver.resolver(request).orElse(null);
        RateLimitService.Identidade id = service.identificar(request, chave);

        // 1) limite LOCAL (do cliente)
        TokenBucket.Resultado r = service.consumir(id);
        response.setHeader("X-RateLimit-Limit", String.valueOf(id.capacidade()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(r.restantes()));
        response.setHeader("X-RateLimit-Reset", String.valueOf(r.resetSegundos()));
        response.setHeader("X-RateLimit-Policy",
                id.politica() + ";capacidade=" + id.capacidade() + ";recarga=" + id.recargaPorMinuto() + "/min");

        if (!r.permitido()) {
            response.setHeader("Retry-After", String.valueOf(r.retryAfter()));
            String msg = r.penalizado()
                    ? "Acesso bloqueado temporariamente por excesso de requisicoes depois do limite (" + id.descricao()
                            + "). Aguarde " + r.retryAfter() + "s. Repetir o abuso pode levar ao bloqueio da chave."
                    : "Limite de requisicoes excedido para " + id.descricao() + " (plano " + id.politica() + ": "
                            + id.capacidade() + " de uma vez, recarga de " + id.recargaPorMinuto()
                            + " por minuto). Tente novamente em " + r.retryAfter() + "s.";
            errorWriter.write(response, HttpStatus.TOO_MANY_REQUESTS, msg);
            return;
        }

        // 2) limite GLOBAL (da API inteira)
        TokenBucket.Resultado g = service.consumirGlobal();
        if (!g.permitido()) {
            response.setHeader("Retry-After", String.valueOf(g.retryAfter()));
            errorWriter.write(response, HttpStatus.TOO_MANY_REQUESTS,
                    "A API esta recebendo muitas requisicoes no momento (limite global). Tente novamente em "
                            + g.retryAfter() + "s.");
            return;
        }
        chain.doFilter(request, response);
    }
}
