package com.floricultura.api.entity;

// plano de uso ligado a chave de API: define o tamanho do "balde" do rate limit (token bucket).
//   capacidade        -> quantas requisicoes da pra fazer de uma vez (rajada)
//   recargaPorMinuto  -> quantas fichas voltam pro balde por minuto (uso continuo)
// quem acessa SEM chave cai no limite anonimo (por IP), configurado no application.properties.
// os numeros sao baixos de proposito pra dar pra ver o 429 em sala (clicar ~10x no Swagger ja trava).
public enum Plano {

    GRATUITO(8, 20),
    BASICO(20, 60),
    PROFISSIONAL(50, 240);

    private final int capacidade;
    private final int recargaPorMinuto;

    Plano(int capacidade, int recargaPorMinuto) {
        this.capacidade = capacidade;
        this.recargaPorMinuto = recargaPorMinuto;
    }

    public int getCapacidade() {
        return capacidade;
    }

    public int getRecargaPorMinuto() {
        return recargaPorMinuto;
    }
}
