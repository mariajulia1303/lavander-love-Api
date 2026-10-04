package com.floricultura.api.dto;

import jakarta.validation.constraints.NotBlank;

// corpo pra criar/atualizar categoria
public class CategoriaRequest {

    @NotBlank
    private String nome;

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
