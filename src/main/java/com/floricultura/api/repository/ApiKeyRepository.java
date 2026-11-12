package com.floricultura.api.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.floricultura.api.entity.ApiKey;
import com.floricultura.api.entity.NivelAcesso;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

    // usado na autenticacao: procura pelo hash da chave recebida no header
    Optional<ApiKey> findByHash(String hash);

    boolean existsByHash(String hash);

    // consultas personalizadas
    Page<ApiKey> findByNivel(NivelAcesso nivel, Pageable pageable);

    Page<ApiKey> findByAtiva(boolean ativa, Pageable pageable);

    Page<ApiKey> findByNivelAndAtiva(NivelAcesso nivel, boolean ativa, Pageable pageable);

    long countByNivelAndAtiva(NivelAcesso nivel, boolean ativa);
}
