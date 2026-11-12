package com.floricultura.api.ratelimit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.floricultura.api.ratelimit.TokenBucket.Resultado;

// testes do algoritmo de rate limit (token bucket) sem precisar subir o Spring
class TokenBucketTest {

    private static final long T = 1_000_000;

    // plano GRATUITO: 8 fichas, 20 por minuto (1 a cada 3 s), penalidade apos 5 negadas = 30 s
    private TokenBucket gratuito() {
        return new TokenBucket(8, 20, 5, 30, T);
    }

    @Test
    void permiteRajadaAteACapacidade() {
        TokenBucket b = gratuito();
        for (int i = 1; i <= 8; i++) {
            Resultado r = b.consumir(T);
            assertTrue(r.permitido());
            assertEquals(8 - i, r.restantes());
        }
    }

    @Test
    void negaQuandoOBaldeEsvaziaEInformaRetryAfter() {
        TokenBucket b = gratuito();
        for (int i = 0; i < 8; i++) {
            b.consumir(T);
        }
        Resultado r = b.consumir(T);
        assertFalse(r.permitido());
        assertFalse(r.penalizado());
        assertEquals(3, r.retryAfter());     // proxima ficha em 3 s
        assertEquals(24, r.resetSegundos()); // 8 fichas * 3 s pra encher
    }

    @Test
    void recarregaAosPoucos() {
        TokenBucket b = gratuito();
        for (int i = 0; i < 8; i++) {
            b.consumir(T);
        }
        assertTrue(b.consumir(T + 3000).permitido());
        assertFalse(b.consumir(T + 3000).permitido());
    }

    @Test
    void insistirDepoisDo429GeraPenalidade() {
        TokenBucket b = gratuito();
        for (int i = 0; i < 8; i++) {
            b.consumir(T);
        }
        Resultado ultimo = null;
        for (int i = 0; i < 5; i++) {
            ultimo = b.consumir(T + 100);
        }
        assertTrue(ultimo.penalizado());
        assertEquals(30, ultimo.retryAfter());
        // mesmo com o balde ja recarregado, continua bloqueado ate acabar a penalidade
        assertFalse(b.consumir(T + 25_000).permitido());
        assertTrue(b.consumir(T + 31_000).permitido());
    }

    @Test
    void naoPassaDaCapacidade() {
        TokenBucket b = gratuito();
        Resultado r = b.espiar(T + 10_000_000);
        assertEquals(8, r.restantes());
        assertEquals(0, r.resetSegundos());
    }
}
