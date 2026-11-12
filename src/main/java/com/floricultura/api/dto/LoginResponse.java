package com.floricultura.api.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

// resposta do login: token da sessao (vai no header X-Session-Token) + dados da conta
@Schema(description = "Token de sessao e dados da conta")
public record LoginResponse(
        @Schema(description = "Envie no header X-Session-Token nas rotas /auth", example = "q3Z...")
        String token,
        @Schema(description = "Quando a sessao expira") Instant expiraEm,
        UsuarioResponse usuario) {
}
