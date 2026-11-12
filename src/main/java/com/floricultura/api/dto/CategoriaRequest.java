package com.floricultura.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// corpo pra criar/atualizar categoria
public class CategoriaRequest {

    @NotBlank
    @Size(min = 2, max = 60, message = "deve ter entre 2 e 60 caracteres")
    private String nome;

    @Size(max = 255, message = "deve ter no maximo 255 caracteres")
    private String descricao;

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }
}
