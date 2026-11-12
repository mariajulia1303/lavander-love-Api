package com.floricultura.api.dto;

import java.time.LocalDateTime;

import org.springframework.hateoas.server.core.Relation;

import com.floricultura.api.entity.ApiKey;
import com.floricultura.api.entity.NivelAcesso;
import com.floricultura.api.entity.Plano;

import io.swagger.v3.oas.annotations.media.Schema;

// resposta da criacao/rotacao de chave: e o UNICO momento em que o valor puro da chave aparece
@Relation(collectionRelation = "apiKeys", itemRelation = "apiKey")
@Schema(description = "Chave recem-gerada. Guarde o campo 'chave': ele nao sera mostrado novamente.")
public class ApiKeyCriadaResponse {

    private Long id;
    private String nome;
    private NivelAcesso nivel;
    private Plano plano;
    private String prefixo;
    @Schema(example = "ll_3kq9X2pVw8mN1rT5yB7cD0eF4gH6jL")
    private String chave;
    private LocalDateTime criadaEm;
    private String aviso = "Guarde esta chave agora: por seguranca ela nao sera exibida novamente.";

    public static ApiKeyCriadaResponse of(ApiKey apiKey, String chavePura) {
        ApiKeyCriadaResponse r = new ApiKeyCriadaResponse();
        r.id = apiKey.getId();
        r.nome = apiKey.getNome();
        r.nivel = apiKey.getNivel();
        r.plano = apiKey.getPlano();
        r.prefixo = apiKey.getPrefixo();
        r.chave = chavePura;
        r.criadaEm = apiKey.getCriadaEm();
        return r;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public NivelAcesso getNivel() {
        return nivel;
    }

    public Plano getPlano() {
        return plano;
    }

    public String getPrefixo() {
        return prefixo;
    }

    public String getChave() {
        return chave;
    }

    public LocalDateTime getCriadaEm() {
        return criadaEm;
    }

    public String getAviso() {
        return aviso;
    }
}
