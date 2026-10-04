package com.floricultura.api.repository;

import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.floricultura.api.entity.Produto;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    // todos os produtos de uma categoria
    Page<Produto> findByCategoriaId(Long categoriaId, Pageable pageable);

    // produtos numa faixa de preco
    Page<Produto> findByPrecoBetween(BigDecimal min, BigDecimal max, Pageable pageable);

    // busca por nome
    Page<Produto> findByNomeContainingIgnoreCase(String nome, Pageable pageable);

    // quantos produtos tem numa categoria (util pra nao deixar deletar categoria com produto)
    long countByCategoriaId(Long categoriaId);
}
