package com.floricultura.api.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.floricultura.api.entity.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    // usado quando um ADMIN revoga uma chave: desliga ela do dono antes de apagar
    Optional<Usuario> findByApiKeyId(Long apiKeyId);
}
