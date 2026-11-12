package com.floricultura.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// corpo pra criar/atualizar endereco (via cliente ou direto)
public class EnderecoRequest {

    @NotBlank
    @Size(min = 3, max = 120, message = "deve ter entre 3 e 120 caracteres")
    private String logradouro;

    // numero pode ser "S/N", entao nao exijo nada aqui
    @Size(max = 10, message = "deve ter no maximo 10 caracteres")
    private String numero;

    @NotBlank
    @Size(min = 2, max = 60, message = "deve ter entre 2 e 60 caracteres")
    private String bairro;

    @NotBlank
    @Size(min = 2, max = 60, message = "deve ter entre 2 e 60 caracteres")
    private String cidade;

    @NotBlank
    @Size(min = 2, max = 2)
    private String uf;

    @NotBlank
    @Size(min = 8, max = 9, message = "deve ter entre 8 e 9 caracteres")
    private String cep;

    public String getLogradouro() {
        return logradouro;
    }

    public void setLogradouro(String logradouro) {
        this.logradouro = logradouro;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public String getBairro() {
        return bairro;
    }

    public void setBairro(String bairro) {
        this.bairro = bairro;
    }

    public String getCidade() {
        return cidade;
    }

    public void setCidade(String cidade) {
        this.cidade = cidade;
    }

    public String getUf() {
        return uf;
    }

    public void setUf(String uf) {
        this.uf = uf;
    }

    public String getCep() {
        return cep;
    }

    public void setCep(String cep) {
        this.cep = cep;
    }
}
