package com.floricultura.api.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

// corpo pra criar/atualizar produto, a categoria vem so pelo id
public class ProdutoRequest {

    @NotBlank
    private String nome;

    @NotNull
    @Positive
    private BigDecimal preco;

    @NotNull
    @PositiveOrZero
    private Integer estoque;

    // id da categoria; o service resolve e devolve 409 se nao existir
    @NotNull
    private Long categoriaId;

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public void setPreco(BigDecimal preco) {
        this.preco = preco;
    }

    public Integer getEstoque() {
        return estoque;
    }

    public void setEstoque(Integer estoque) {
        this.estoque = estoque;
    }

    public Long getCategoriaId() {
        return categoriaId;
    }

    public void setCategoriaId(Long categoriaId) {
        this.categoriaId = categoriaId;
    }
}
