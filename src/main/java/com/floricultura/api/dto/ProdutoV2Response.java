package com.floricultura.api.dto;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

import org.springframework.hateoas.server.core.Relation;

import com.floricultura.api.entity.Produto;

import io.swagger.v3.oas.annotations.media.Schema;

// formato da VERSAO 2 do produto (header X-API-Version: 2).
// diferencas pra v1:
//  - categoria vem resumida (so id e nome), em vez do objeto completo
//  - novo campo precoFormatado ("R$ 89,90"), pronto pra mostrar na tela
//  - novo campo disponivel (true quando tem estoque)
@Relation(collectionRelation = "produtos", itemRelation = "produto")
@Schema(name = "ProdutoV2", description = "Produto no formato da versao 2 da API")
public class ProdutoV2Response {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private Long id;
    private String nome;
    private BigDecimal preco;
    @Schema(example = "R$ 89,90")
    private String precoFormatado;
    private Integer estoque;
    private boolean disponivel;
    private CategoriaResumo categoria;

    public static ProdutoV2Response from(Produto p) {
        ProdutoV2Response r = new ProdutoV2Response();
        r.id = p.getId();
        r.nome = p.getNome();
        r.preco = p.getPreco();
        r.precoFormatado = p.getPreco() == null ? null
                : NumberFormat.getCurrencyInstance(PT_BR).format(p.getPreco()).replace(' ', ' ');
        r.estoque = p.getEstoque();
        r.disponivel = p.getEstoque() != null && p.getEstoque() > 0;
        if (p.getCategoria() != null) {
            r.categoria = new CategoriaResumo(p.getCategoria().getId(), p.getCategoria().getNome());
        }
        return r;
    }

    @Schema(name = "CategoriaResumo")
    public static class CategoriaResumo {
        private final Long id;
        private final String nome;

        public CategoriaResumo(Long id, String nome) {
            this.id = id;
            this.nome = nome;
        }

        public Long getId() {
            return id;
        }

        public String getNome() {
            return nome;
        }
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public String getPrecoFormatado() {
        return precoFormatado;
    }

    public Integer getEstoque() {
        return estoque;
    }

    public boolean isDisponivel() {
        return disponivel;
    }

    public CategoriaResumo getCategoria() {
        return categoria;
    }
}
