package com.floricultura.api.assembler;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import com.floricultura.api.controller.ApiKeyController;
import com.floricultura.api.dto.ApiKeyCriadaResponse;
import com.floricultura.api.entity.ApiKey;

// monta os links HATEOAS de uma chave de API
@Component
public class ApiKeyAssembler implements RepresentationModelAssembler<ApiKey, EntityModel<ApiKey>> {

    @Override
    @NonNull
    public EntityModel<ApiKey> toModel(@NonNull ApiKey apiKey) {
        Long id = apiKey.getId();
        return EntityModel.of(apiKey,
                linkTo(methodOn(ApiKeyController.class).buscar(id)).withSelfRel(),
                linkTo(methodOn(ApiKeyController.class).atualizar(id, null, null)).withRel("update"),
                linkTo(methodOn(ApiKeyController.class).rotacionar(id)).withRel("rotacionar"),
                linkTo(methodOn(ApiKeyController.class).revogar(id, null)).withRel("delete"),
                linkTo(methodOn(ApiKeyController.class).listar(Pageable.unpaged())).withRel("apiKeys"));
    }

    // a resposta de criacao tem o valor puro da chave, entao e outro tipo, mas com os mesmos links
    public EntityModel<ApiKeyCriadaResponse> toModel(ApiKeyCriadaResponse criada) {
        Long id = criada.getId();
        return EntityModel.of(criada,
                linkTo(methodOn(ApiKeyController.class).buscar(id)).withSelfRel(),
                linkTo(methodOn(ApiKeyController.class).atualizar(id, null, null)).withRel("update"),
                linkTo(methodOn(ApiKeyController.class).revogar(id, null)).withRel("delete"),
                linkTo(methodOn(ApiKeyController.class).listar(Pageable.unpaged())).withRel("apiKeys"));
    }
}
