package com.floricultura.api.dto;

import com.floricultura.api.entity.NivelAcesso;
import com.floricultura.api.entity.Plano;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// corpo pra gerar uma nova chave de API
@Schema(description = "Dados para gerar uma chave de API")
public class ApiKeyRequest {

    @NotBlank
    @Size(min = 3, max = 80)
    @Schema(description = "Nome que identifica quem vai usar a chave", example = "Front-end da loja")
    private String nome;

    @NotNull
    @Schema(description = "Nivel de acesso da chave", example = "ESCRITA")
    private NivelAcesso nivel;

    @Schema(description = "Plano de uso (define o rate limit). Opcional, padrao GRATUITO.", example = "BASICO")
    private Plano plano;

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public NivelAcesso getNivel() {
        return nivel;
    }

    public void setNivel(NivelAcesso nivel) {
        this.nivel = nivel;
    }

    public Plano getPlano() {
        return plano;
    }

    public void setPlano(Plano plano) {
        this.plano = plano;
    }
}
