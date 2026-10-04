package com.floricultura.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// corpo pra criar/atualizar cliente, com o endereco aninhado
public class ClienteRequest {

    @NotBlank
    private String nome;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String telefone;

    // endereco vem junto no mesmo body, por isso o @Valid pra validar os campos dele tambem
    @NotNull
    @Valid
    private EnderecoRequest endereco;

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public EnderecoRequest getEndereco() {
        return endereco;
    }

    public void setEndereco(EnderecoRequest endereco) {
        this.endereco = endereco;
    }
}
