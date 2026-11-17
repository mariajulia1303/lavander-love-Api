package com.floricultura.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// dados pra criar uma conta no painel
@Schema(description = "Dados para criar uma conta")
public record CadastroRequest(
        @NotBlank
        @Size(min = 3, max = 100, message = "deve ter entre 3 e 100 caracteres")
        String nome,

        @NotBlank
        @Email
        @Size(max = 120, message = "deve ter no maximo 120 caracteres")
        String email,

        @NotBlank
        @Size(min = 6, max = 72, message = "deve ter entre 6 e 72 caracteres")
        String senha) {
}
