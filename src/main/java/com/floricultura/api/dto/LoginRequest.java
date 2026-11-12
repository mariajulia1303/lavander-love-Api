package com.floricultura.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "E-mail e senha para entrar")
public record LoginRequest(
        @NotBlank
        @Size(max = 120, message = "deve ter no maximo 120 caracteres")
        @Schema(example = "maria@email.com")
        String email,

        @NotBlank
        @Size(max = 72, message = "deve ter no maximo 72 caracteres")
        @Schema(example = "minhaSenha123")
        String senha) {
}
