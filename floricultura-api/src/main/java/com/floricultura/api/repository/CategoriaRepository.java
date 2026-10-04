package com.floricultura.api.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.floricultura.api.entity.Categoria;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    // busca por nome
    Page<Categoria> findByNomeContainingIgnoreCase(String nome, Pageable pageable);

    // checa duplicidade de nome antes de salvar
    boolean existsByNome(String nome);
}
