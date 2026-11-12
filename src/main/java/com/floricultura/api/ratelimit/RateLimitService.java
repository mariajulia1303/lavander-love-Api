package com.floricultura.api.ratelimit;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.floricultura.api.entity.ApiKey;
import com.floricultura.api.entity.Plano;

import jakarta.servlet.http.HttpServletRequest;

// guarda os baldes (token bucket) de cada cliente, em memoria.
//
// rate limit em dois niveis (como visto em aula):
//  - LOCAL, por cliente:
//      com chave de API valida -> balde da CHAVE, do tamanho do plano dela (GRATUITO/BASICO/PROFISSIONAL)
//      sem chave               -> balde do IP (limite "anonimo", o menor de todos)
//  - GLOBAL: um balde unico pra API inteira, pra ela nao cair se muita gente vier junto (ex: DDoS)
//
// obs: como e em memoria, os baldes zeram quando a app reinicia e nao sao compartilhados
// entre varias instancias. numa API distribuida isso iria pro Redis (ex: Bucket4j + Redis).
@Service
public class RateLimitService {

    private final Map<String, TokenBucket> baldes = new ConcurrentHashMap<>();
    private volatile TokenBucket global;

    @Value("${lavander.rate-limit.anonimo.capacidade:5}")
    private int anonimoCapacidade;

    @Value("${lavander.rate-limit.anonimo.recarga-por-minuto:15}")
    private int anonimoRecarga;

    @Value("${lavander.rate-limit.global.capacidade:200}")
    private int globalCapacidade;

    @Value("${lavander.rate-limit.global.recarga-por-minuto:600}")
    private int globalRecarga;

    @Value("${lavander.rate-limit.penalidade.tentativas:5}")
    private int penalidadeTentativas;

    @Value("${lavander.rate-limit.penalidade.segundos:30}")
    private long penalidadeSegundos;

    // quem e o cliente desta requisicao e qual regra vale pra ele
    public Identidade identificar(HttpServletRequest request, ApiKey chave) {
        if (chave != null) {
            Plano plano = chave.getPlano() != null ? chave.getPlano() : Plano.GRATUITO;
            // o plano entra no id do balde: se o plano da chave mudar, ela ganha um balde novo do tamanho certo
            return new Identidade("chave:" + chave.getId() + ":" + plano, "Chave " + chave.getPrefixo(),
                    plano.name(), plano.getCapacidade(), plano.getRecargaPorMinuto());
        }
        String ip = ip(request);
        return new Identidade("ip:" + ip, "IP " + ip, "ANONIMO", anonimoCapacidade, anonimoRecarga);
    }

    public TokenBucket.Resultado consumir(Identidade id) {
        long agora = System.currentTimeMillis();
        limparSeNecessario(agora);
        return balde(id, agora).consumir(agora);
    }

    public TokenBucket.Resultado espiar(Identidade id) {
        long agora = System.currentTimeMillis();
        return balde(id, agora).espiar(agora);
    }

    public TokenBucket.Resultado consumirGlobal() {
        return global().consumir(System.currentTimeMillis());
    }

    public int getAnonimoCapacidade() {
        return anonimoCapacidade;
    }

    public int getAnonimoRecarga() {
        return anonimoRecarga;
    }

    public int getGlobalCapacidade() {
        return globalCapacidade;
    }

    public int getGlobalRecarga() {
        return globalRecarga;
    }

    public int getPenalidadeTentativas() {
        return penalidadeTentativas;
    }

    public long getPenalidadeSegundos() {
        return penalidadeSegundos;
    }

    private TokenBucket balde(Identidade id, long agora) {
        return baldes.computeIfAbsent(id.chave(), k -> new TokenBucket(id.capacidade(), id.recargaPorMinuto(),
                penalidadeTentativas, penalidadeSegundos, agora));
    }

    private TokenBucket global() {
        TokenBucket g = global;
        if (g == null) {
            synchronized (this) {
                if (global == null) {
                    // o global nao tem penalidade: ele nao sabe quem e o culpado
                    global = new TokenBucket(globalCapacidade, globalRecarga, 0, 0, System.currentTimeMillis());
                }
                g = global;
            }
        }
        return g;
    }

    // atras de proxy (ex: Render) o IP real vem no X-Forwarded-For
    private static String ip(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    // evita o mapa crescer pra sempre: joga fora baldes parados ha mais de 10 min
    private void limparSeNecessario(long agora) {
        if (baldes.size() > 10_000) {
            baldes.entrySet().removeIf(e -> agora - e.getValue().getUltimoUso() > 600_000);
        }
    }

    // chave -> id interno do balde; descricao -> texto amigavel; politica -> nome do plano
    public record Identidade(String chave, String descricao, String politica, int capacidade,
            int recargaPorMinuto) {
    }
}
