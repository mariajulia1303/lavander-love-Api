package com.floricultura.api.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.floricultura.api.dto.CategoriaRequest;
import com.floricultura.api.entity.Categoria;
import com.floricultura.api.exception.ConflictException;
import com.floricultura.api.exception.ResourceNotFoundException;
import com.floricultura.api.repository.CategoriaRepository;
import com.floricultura.api.repository.ProdutoRepository;

// regras de negocio das categorias
@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final ProdutoRepository produtoRepository;

    public CategoriaService(CategoriaRepository categoriaRepository, ProdutoRepository produtoRepository) {
        this.categoriaRepository = categoriaRepository;
        this.produtoRepository = produtoRepository;
    }

    // cria categoria; se o nome ja existe devolve 409
    public Categoria create(CategoriaRequest req) {
        if (categoriaRepository.existsByNome(req.getNome())) {
            throw new ConflictException("Ja existe uma categoria com o nome '" + req.getNome() + "'");
        }
        Categoria c = new Categoria();
        c.setNome(req.getNome());
        c.setDescricao(req.getDescricao());
        return categoriaRepository.save(c);
    }

    public Categoria findById(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria " + id + " nao encontrada"));
    }

    public Page<Categoria> findAll(Pageable pageable) {
        return categoriaRepository.findAll(pageable);
    }

    // consulta personalizada: busca por nome
    public Page<Categoria> search(String nome, Pageable pageable) {
        return categoriaRepository.findByNomeContainingIgnoreCase(nome, pageable);
    }

    public Categoria update(Long id, CategoriaRequest req) {
        Categoria c = findById(id);
        // se mudou o nome pra um que ja existe em outra categoria, 409
        if (!c.getNome().equals(req.getNome()) && categoriaRepository.existsByNome(req.getNome())) {
            throw new ConflictException("Ja existe uma categoria com o nome '" + req.getNome() + "'");
        }
        c.setNome(req.getNome());
        c.setDescricao(req.getDescricao());
        return categoriaRepository.save(c);
    }

    public void delete(Long id) {
        Categoria c = findById(id);
        // nao deixa apagar categoria que ainda tem produtos pendurados nela -> 409
        if (produtoRepository.countByCategoriaId(id) > 0) {
            throw new ConflictException("Categoria " + id + " ainda possui produtos e nao pode ser removida");
        }
        categoriaRepository.delete(c);
    }
}
