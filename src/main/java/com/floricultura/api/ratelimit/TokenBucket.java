package com.floricultura.api.ratelimit;

// algoritmo TOKEN BUCKET (balde de fichas), o mesmo usado pela biblioteca Bucket4j.
//
// cada cliente tem um "balde" com N fichas (capacidade). cada requisicao gasta 1 ficha.
// o balde vai sendo reabastecido aos poucos (ex: 20 fichas por minuto = 1 a cada 3 s).
//  - trafego normal: sempre tem ficha, tudo passa;
//  - rajada curta (pico): o cliente pode gastar o balde inteiro de uma vez;
//  - balde vazio: 429 ate cair a proxima ficha (Retry-After diz em quantos segundos).
// diferente da janela fixa, aqui nao existe o "estouro na virada da janela": a recarga e continua.
//
// PENALIDADE: quem continua insistindo depois de receber 429 (N tentativas negadas seguidas)
// fica bloqueado por alguns segundos, mesmo que caia ficha nova no balde.
//
// classe Java pura (sem Spring) de proposito: da pra testar isolada.
public final class TokenBucket {

    private final int capacidade;
    private final double fichasPorMs;
    private final int tentativasAtePenalidade; // 0 = sem penalidade
    private final long penalidadeMs;

    private double fichas;
    private long ultimaRecarga;
    private int negadasSeguidas;
    private long bloqueadoAte;
    private volatile long ultimoUso;

    public TokenBucket(int capacidade, int recargaPorMinuto, int tentativasAtePenalidade, long penalidadeSegundos,
            long agora) {
        if (capacidade < 1 || recargaPorMinuto < 1) {
            throw new IllegalArgumentException("capacidade e recarga precisam ser >= 1");
        }
        this.capacidade = capacidade;
        this.fichasPorMs = recargaPorMinuto / 60_000.0;
        this.tentativasAtePenalidade = Math.max(0, tentativasAtePenalidade);
        this.penalidadeMs = Math.max(0, penalidadeSegundos) * 1000;
        this.fichas = capacidade; // comeca cheio
        this.ultimaRecarga = agora;
        this.ultimoUso = agora;
    }

    // tenta gastar 1 ficha
    public synchronized Resultado consumir(long agora) {
        ultimoUso = agora;
        recarregar(agora);

        if (agora < bloqueadoAte) {
            return new Resultado(false, 0, segundos(bloqueadoAte - agora), segundosAteEncher(), true);
        }
        if (fichas >= 1) {
            fichas -= 1;
            negadasSeguidas = 0;
            return new Resultado(true, (int) Math.floor(fichas), 0, segundosAteEncher(), false);
        }

        negadasSeguidas++;
        if (tentativasAtePenalidade > 0 && negadasSeguidas >= tentativasAtePenalidade) {
            bloqueadoAte = agora + penalidadeMs;
            negadasSeguidas = 0;
            return new Resultado(false, 0, segundos(penalidadeMs), segundosAteEncher(), true);
        }
        long msAteProxima = (long) Math.ceil((1 - fichas) / fichasPorMs);
        return new Resultado(false, 0, segundos(msAteProxima), segundosAteEncher(), false);
    }

    // so consulta, sem gastar ficha
    public synchronized Resultado espiar(long agora) {
        recarregar(agora);
        boolean bloqueado = agora < bloqueadoAte;
        return new Resultado(!bloqueado && fichas >= 1, bloqueado ? 0 : (int) Math.floor(fichas),
                bloqueado ? segundos(bloqueadoAte - agora) : 0, segundosAteEncher(), bloqueado);
    }

    public int getCapacidade() {
        return capacidade;
    }

    public long getUltimoUso() {
        return ultimoUso;
    }

    private void recarregar(long agora) {
        long passou = agora - ultimaRecarga;
        if (passou > 0) {
            fichas = Math.min(capacidade, fichas + passou * fichasPorMs);
            ultimaRecarga = agora;
        }
    }

    private long segundosAteEncher() {
        if (fichas >= capacidade) {
            return 0;
        }
        return segundos((long) Math.ceil((capacidade - fichas) / fichasPorMs));
    }

    private static long segundos(long ms) {
        return Math.max(1, (ms + 999) / 1000);
    }

    // permitido      -> a requisicao pode seguir
    // restantes      -> fichas que sobraram (X-RateLimit-Remaining)
    // retryAfter     -> segundos ate poder tentar de novo (so quando negado)
    // resetSegundos  -> segundos ate o balde encher de novo (X-RateLimit-Reset)
    // penalizado     -> negado por insistir depois do 429 (bloqueio temporario)
    public record Resultado(boolean permitido, int restantes, long retryAfter, long resetSegundos,
            boolean penalizado) {
    }
}
