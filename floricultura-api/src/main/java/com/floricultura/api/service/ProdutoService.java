package com.floricultura.api.service;

import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.floricultura.api.dto.ProdutoRequest;
import com.floricultura.api.entity.Categoria;
import com.floricultura.api.entity.Produto;
import com.floricultura.api.exception.ConflictException;
import com.floricultura.api.exception.ResourceNotFoundException;
import com.floricultura.api.repository.CategoriaRepository;
import com.floricultura.api.repository.ProdutoRepository;

// regras de negocio dos produtos
@Service
public class ProdutoService {

    private final ProdutoRepository produtoRepository;
    private final CategoriaRepository categoriaRepository;

    public ProdutoService(ProdutoRepository produtoRepository, CategoriaRepository categoriaRepository) {
        this.produtoRepository = produtoRepository;
        this.categoriaRepository = categoriaRepository;
    }

    public Produto create(ProdutoRequest req) {
        Produto p = new Produto();
        p.setNome(req.getNome());
        p.setPreco(req.getPreco());
        p.setEstoque(req.getEstoque());
        // resolve a categoria pelo id; POST nunca da 404, se faltar -> 409
        p.setCategoria(resolveCategoria(req.getCategoriaId()));
        return produtoRepository.save(p);
    }

    public Produto findById(Long id) {
        return produtoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produto " + id + " nao encontrado"));
    }

    public Page<Produto> findAll(Pageable pageable) {
        return produtoRepository.findAll(pageable);
    }

    // consultas personalizadas
    public Page<Produto> findByCategoria(Long categoriaId, Pageable pageable) {
        return produtoRepository.findByCategoriaId(categoriaId, pageable);
    }

    public Page<Produto> findByFaixaPreco(BigDecimal min, BigDecimal max, Pageable pageable) {
        return produtoRepository.findByPrecoBetween(min, max, pageable);
    }

    public Page<Produto> search(String nome, Pageable pageable) {
        return produtoRepository.findByNomeContainingIgnoreCase(nome, pageable);
    }

    public Produto update(Long id, ProdutoRequest req) {
        Produto p = findById(id);
        p.setNome(req.getNome());
        p.setPreco(req.getPreco());
        p.setEstoque(req.getEstoque());
        p.setCategoria(resolveCategoria(req.getCategoriaId()));
        return produtoRepository.save(p);
    }

    public void delete(Long id) {
        Produto p = findById(id);
        produtoRepository.delete(p);
    }

    // busca a categoria; se nao achar vira conflito (409) e nao 404, por ser id relacionado
    private Categoria resolveCategoria(Long categoriaId) {
        return categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new ConflictException("Categoria " + categoriaId + " nao existe"));
    }
}
