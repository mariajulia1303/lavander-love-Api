package com.floricultura.api.dto;

import java.time.LocalDateTime;

import org.springframework.hateoas.server.core.Relation;

import com.floricultura.api.entity.ApiKey;
import com.floricultura.api.entity.NivelAcesso;
import com.floricultura.api.entity.Plano;
import com.floricultura.api.entity.Usuario;

import io.swagger.v3.oas.annotations.media.Schema;

// dados da conta + um resumo da chave pessoal (sem o valor da chave, que so aparece ao gerar)
@Relation(itemRelation = "usuario", collectionRelation = "usuarios")
@Schema(description = "Dados da conta e resumo da chave de API pessoal")
public record UsuarioResponse(
        Long id,
        String nome,
        String email,
        LocalDateTime criadoEm,
        @Schema(description = "null enquanto a pessoa nao gerou a chave") ChaveResumo chave) {

    public static UsuarioResponse from(Usuario u) {
        ApiKey k = u.getApiKey();
        ChaveResumo resumo = k == null ? null
                : new ChaveResumo(k.getId(), k.getPrefixo(), k.getNivel(), k.getPlano(), k.isAtiva(), k.getUltimoUso());
        return new UsuarioResponse(u.getId(), u.getNome(), u.getEmail(), u.getCriadoEm(), resumo);
    }

    public record ChaveResumo(Long id, String prefixo, NivelAcesso nivel, Plano plano, boolean ativa,
            LocalDateTime ultimoUso) {
    }
}
