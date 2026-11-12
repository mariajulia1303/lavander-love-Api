package com.floricultura.api.dto;

import com.floricultura.api.entity.NivelAcesso;
import com.floricultura.api.entity.Plano;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// corpo pra atualizar uma chave (nome, nivel e se esta ativa). o valor da chave nao muda aqui.
@Schema(description = "Dados para atualizar uma chave de API")
public class ApiKeyUpdateRequest {

    @NotBlank
    @Size(min = 3, max = 80)
    @Schema(example = "Front-end da loja")
    private String nome;

    @NotNull
    @Schema(example = "LEITURA")
    private NivelAcesso nivel;

    @Schema(description = "Plano de uso (rate limit). Opcional: se nao vier, mantem o atual.", example = "PROFISSIONAL")
    private Plano plano;

    @NotNull
    @Schema(description = "false desativa a chave sem apagar", example = "true")
    private Boolean ativa;

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

    public Boolean getAtiva() {
        return ativa;
    }

    public void setAtiva(Boolean ativa) {
        this.ativa = ativa;
    }

    public Plano getPlano() {
        return plano;
    }

    public void setPlano(Plano plano) {
        this.plano = plano;
    }
}
