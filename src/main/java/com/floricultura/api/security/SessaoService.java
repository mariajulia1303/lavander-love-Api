package com.floricultura.api.security;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

// sessao de login (bem simples, em memoria): depois do login a pessoa recebe um token aleatorio
// e manda ele no header X-Session-Token pra ver a conta e gerar a chave de API pessoal.
// o token NAO substitui a chave de API: ele so vale nas rotas /auth (a "area da conta").
// expira depois de alguns minutos (lavander.auth.sessao-minutos) ou no logout.
@Service
public class SessaoService {

    public static final String HEADER = "X-Session-Token";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Map<String, Sessao> sessoes = new ConcurrentHashMap<>();

    @Value("${lavander.auth.sessao-minutos:60}")
    private long minutos;

    public Sessao abrir(Long usuarioId) {
        limparVencidas();
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Sessao s = new Sessao(token, usuarioId, Instant.now().plusSeconds(minutos * 60));
        sessoes.put(token, s);
        return s;
    }

    public Optional<Sessao> buscar(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        Sessao s = sessoes.get(token.trim());
        if (s == null) {
            return Optional.empty();
        }
        if (s.expiraEm().isBefore(Instant.now())) {
            sessoes.remove(s.token());
            return Optional.empty();
        }
        return Optional.of(s);
    }

    public void fechar(String token) {
        if (token != null) {
            sessoes.remove(token.trim());
        }
    }

    private void limparVencidas() {
        if (sessoes.size() > 1000) {
            Instant agora = Instant.now();
            sessoes.values().removeIf(s -> s.expiraEm().isBefore(agora));
        }
    }

    public record Sessao(String token, Long usuarioId, Instant expiraEm) {
    }
}
