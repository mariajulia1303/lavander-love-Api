package com.floricultura.api.idempotency;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import com.floricultura.api.security.ApiKeyResolver;
import com.floricultura.api.web.ApiPaths;
import com.floricultura.api.web.ErrorResponseWriter;
import com.floricultura.api.web.FilterOrder;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// idempotencia nos POST com o header X-Idempotency-Key.
//
// problema: o cliente manda um POST /pedidos, a internet cai antes da resposta chegar e ele
// tenta de novo -> sem idempotencia o pedido seria criado DUAS vezes.
//
// como funciona:
//  1. todo POST precisa mandar X-Idempotency-Key (um UUID gerado pelo cliente) -> senao 400
//  2. primeira vez com aquela chave: processa normal e guarda status + corpo da resposta
//  3. mesma chave + mesmo corpo de novo: NAO processa, devolve a resposta guardada
//     (com o header X-Idempotency-Replayed: true)
//  4. mesma chave + corpo DIFERENTE: 422 (a chave ja foi usada pra outra operacao)
//  5. mesma chave enquanto a primeira ainda esta processando: 409
//  so respostas de sucesso (2xx) ficam guardadas; se deu erro, a chave e liberada pra nova tentativa.
// as chaves sao separadas por cliente (chave de API ou IP) e por rota.
@Component
@Order(FilterOrder.IDEMPOTENCIA)
public class IdempotencyFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Idempotency-Key";
    public static final String HEADER_REPLAY = "X-Idempotency-Replayed";

    private final IdempotencyStore store;
    private final ErrorResponseWriter errorWriter;

    public IdempotencyFilter(IdempotencyStore store, ErrorResponseWriter errorWriter) {
        this.store = store;
        this.errorWriter = errorWriter;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String caminho = ApiPaths.caminho(request);
        // login/cadastro ficam de fora: login nao cria recurso, e guardar a resposta dele guardaria o token
        return !"POST".equalsIgnoreCase(request.getMethod()) || !ApiPaths.ehApi(caminho)
                || ApiPaths.comecaCom(caminho, ApiPaths.CONTA);
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain chain) throws ServletException, IOException {
        String chaveIdem = request.getHeader(HEADER);
        if (chaveIdem == null || chaveIdem.isBlank()) {
            errorWriter.write(response, HttpStatus.BAD_REQUEST,
                    "Operacoes POST exigem o header X-Idempotency-Key (ex: um UUID gerado pelo cliente).");
            return;
        }
        chaveIdem = chaveIdem.trim();
        if (chaveIdem.length() > 100) {
            errorWriter.write(response, HttpStatus.BAD_REQUEST, "X-Idempotency-Key deve ter no maximo 100 caracteres.");
            return;
        }

        CachedBodyRequest req = new CachedBodyRequest(request);
        String hashCorpo = sha256(req.getBody());
        String cliente = ApiKeyResolver.atual(request).map(k -> "chave:" + k.getId())
                .orElse("ip:" + request.getRemoteAddr());
        String chaveStore = cliente + "|" + ApiPaths.caminho(request) + "|" + chaveIdem;

        IdempotencyStore.Registro existente = store.reservar(chaveStore, hashCorpo);
        if (existente != null) {
            responderRepetida(existente, hashCorpo, chaveIdem, response);
            return;
        }

        ContentCachingResponseWrapper resp = new ContentCachingResponseWrapper(response);
        boolean sucesso = false;
        try {
            resp.setHeader(HEADER, chaveIdem);
            chain.doFilter(req, resp);
            int status = resp.getStatus();
            if (status >= 200 && status < 300) {
                store.concluir(chaveStore, status, resp.getContentType(), resp.getHeader("Location"),
                        resp.getContentAsByteArray());
                sucesso = true;
            }
        } finally {
            if (!sucesso) {
                store.liberar(chaveStore);
            }
            resp.copyBodyToResponse();
        }
    }

    private void responderRepetida(IdempotencyStore.Registro r, String hashCorpo, String chaveIdem,
            HttpServletResponse response) throws IOException {
        if (!r.hashCorpo.equals(hashCorpo)) {
            errorWriter.write(response, HttpStatus.UNPROCESSABLE_ENTITY,
                    "A X-Idempotency-Key '" + chaveIdem + "' ja foi usada com um corpo diferente. "
                            + "Gere uma nova chave para uma nova operacao.");
            return;
        }
        if (r.estado != IdempotencyStore.Estado.CONCLUIDA) {
            errorWriter.write(response, HttpStatus.CONFLICT,
                    "Ja existe uma requisicao com a X-Idempotency-Key '" + chaveIdem
                            + "' em processamento. Aguarde e tente novamente.");
            return;
        }
        // repeticao da mesma operacao: devolve exatamente a resposta original, sem processar de novo
        response.setStatus(r.status);
        response.setHeader(HEADER, chaveIdem);
        response.setHeader(HEADER_REPLAY, "true");
        if (r.location != null) {
            response.setHeader("Location", r.location);
        }
        if (r.contentType != null) {
            response.setContentType(r.contentType);
        }
        if (r.corpo != null) {
            response.getOutputStream().write(r.corpo);
        }
    }

    private static String sha256(byte[] dados) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(dados));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
