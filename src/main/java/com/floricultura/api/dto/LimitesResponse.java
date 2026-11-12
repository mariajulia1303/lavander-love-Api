package com.floricultura.api.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

// politica de rate limit da API + situacao atual de quem fez a requisicao
@Schema(description = "Politica de rate limit (token bucket) e o consumo atual de quem chamou")
public record LimitesResponse(
        @Schema(example = "TOKEN_BUCKET") String algoritmo,
        @Schema(description = "Limites por plano de chave de API") List<Faixa> planos,
        @Schema(description = "Limite de quem acessa sem chave (contado por IP)") Faixa anonimo,
        @Schema(description = "Limite da API inteira (soma de todos os clientes)") Faixa global,
        Penalidade penalidade,
        @Schema(description = "Situacao de quem fez esta requisicao") Status voce) {

    public record Faixa(
            @Schema(example = "GRATUITO") String nome,
            @Schema(description = "Requisicoes de uma vez (tamanho do balde)", example = "8") int capacidade,
            @Schema(description = "Fichas que voltam ao balde por minuto", example = "20") int recargaPorMinuto) {
    }

    public record Penalidade(
            @Schema(description = "Requisicoes negadas seguidas que disparam o bloqueio", example = "5") int tentativas,
            @Schema(description = "Duracao do bloqueio temporario", example = "30") long segundos,
            String descricao) {
    }

    public record Status(
            @Schema(example = "Chave ll_admin...") String identificacao,
            @Schema(example = "GRATUITO") String plano,
            int capacidade,
            @Schema(description = "Fichas restantes agora") int restantes,
            @Schema(description = "Segundos ate o balde encher") long resetSegundos,
            @Schema(description = "true = bloqueado por penalidade") boolean bloqueado) {
    }
}
