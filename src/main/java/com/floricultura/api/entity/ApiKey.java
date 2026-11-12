package com.floricultura.api.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// chave de API usada no header X-API-Key.
// a chave em si NUNCA fica salva no banco: guardamos so o hash SHA-256 dela.
// o valor puro aparece uma unica vez, na resposta de criacao (igual GitHub/Stripe fazem).
@Entity
@Table(name = "api_key")
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // nome pra identificar de quem/qual app e a chave (ex: "Front-end da loja")
    @NotBlank
    @Size(min = 3, max = 80)
    private String nome;

    // hash da chave, nunca sai no json
    @JsonIgnore
    @Column(name = "chave_hash", unique = true, nullable = false, length = 64)
    private String hash;

    // comeco da chave, so pra pessoa reconhecer qual e (ex: "ll_Ab3dE...")
    private String prefixo;

    @NotNull
    @Enumerated(EnumType.STRING)
    private NivelAcesso nivel;

    // plano de uso: define o tamanho do balde do rate limit (ver Plano)
    @NotNull
    @Enumerated(EnumType.STRING)
    private Plano plano = Plano.GRATUITO;

    // chave desativada continua no banco mas nao autentica mais
    private boolean ativa = true;

    private LocalDateTime criadaEm;

    private LocalDateTime ultimoUso;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getPrefixo() {
        return prefixo;
    }

    public void setPrefixo(String prefixo) {
        this.prefixo = prefixo;
    }

    public NivelAcesso getNivel() {
        return nivel;
    }

    public void setNivel(NivelAcesso nivel) {
        this.nivel = nivel;
    }

    public Plano getPlano() {
        return plano;
    }

    public void setPlano(Plano plano) {
        this.plano = plano;
    }

    public boolean isAtiva() {
        return ativa;
    }

    public void setAtiva(boolean ativa) {
        this.ativa = ativa;
    }

    public LocalDateTime getCriadaEm() {
        return criadaEm;
    }

    public void setCriadaEm(LocalDateTime criadaEm) {
        this.criadaEm = criadaEm;
    }

    public LocalDateTime getUltimoUso() {
        return ultimoUso;
    }

    public void setUltimoUso(LocalDateTime ultimoUso) {
        this.ultimoUso = ultimoUso;
    }
}
