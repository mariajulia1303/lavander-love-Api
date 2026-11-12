package com.floricultura.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;

// corpo pra criar/atualizar cliente, com o endereco aninhado
public class ClienteRequest {

    @NotBlank
    @Size(min = 3, max = 100, message = "deve ter entre 3 e 100 caracteres")
    private String nome;

    @NotBlank
    @Email
    @Size(max = 120, message = "deve ter no maximo 120 caracteres")
    private String email;

    @NotBlank
    @Size(min = 8, max = 20, message = "deve ter entre 8 e 20 caracteres")
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
