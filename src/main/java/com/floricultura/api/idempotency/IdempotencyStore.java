package com.floricultura.api.idempotency;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// onde guardamos as chaves de idempotencia ja usadas e a resposta que cada uma gerou.
// fica em memoria (ConcurrentHashMap) e cada registro expira depois do TTL configurado.
// numa API com varias instancias isso iria pro Redis ou pro banco, mas a ideia e a mesma.
@Component
public class IdempotencyStore {

    public enum Estado { EM_PROCESSAMENTO, CONCLUIDA }

    public static final class Registro {
        final String hashCorpo;
        final long criadoEm;
        volatile Estado estado = Estado.EM_PROCESSAMENTO;
        volatile int status;
        volatile String contentType;
        volatile String location;
        volatile byte[] corpo;

        Registro(String hashCorpo, long criadoEm) {
            this.hashCorpo = hashCorpo;
            this.criadoEm = criadoEm;
        }
    }

    private final Map<String, Registro> registros = new ConcurrentHashMap<>();
    private final long ttlMs;

    public IdempotencyStore(@Value("${lavander.idempotency.ttl-horas:24}") long ttlHoras) {
        this.ttlMs = Duration.ofHours(ttlHoras).toMillis();
    }

    // tenta "reservar" a chave. se ja existir (e nao expirou) devolve o registro existente,
    // se nao, cria um novo EM_PROCESSAMENTO e devolve null (= pode processar).
    public Registro reservar(String chave, String hashCorpo) {
        long agora = System.currentTimeMillis();
        registros.entrySet().removeIf(e -> agora - e.getValue().criadoEm > ttlMs);
        Registro novo = new Registro(hashCorpo, agora);
        Registro existente = registros.putIfAbsent(chave, novo);
        return existente;
    }

    public void concluir(String chave, int status, String contentType, String location, byte[] corpo) {
        Registro r = registros.get(chave);
        if (r != null) {
            r.status = status;
            r.contentType = contentType;
            r.location = location;
            r.corpo = corpo;
            r.estado = Estado.CONCLUIDA;
        }
    }

    // a operacao falhou: libera a chave pra pessoa poder tentar de novo
    public void liberar(String chave) {
        registros.remove(chave);
    }
}
