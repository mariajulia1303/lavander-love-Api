package com.floricultura.api.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.floricultura.api.entity.Cliente;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    // busca por nome sem ligar pra maiuscula/minuscula, ja paginada
    Page<Cliente> findByNomeContainingIgnoreCase(String nome, Pageable pageable);

    // usado pra checar se ja existe cliente com aquele email
    Optional<Cliente> findByEmail(String email);
}
