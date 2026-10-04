package com.floricultura.api.assembler;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import com.floricultura.api.controller.EnderecoController;
import com.floricultura.api.entity.Endereco;

// monta os links HATEOAS de um endereco
@Component
public class EnderecoAssembler implements RepresentationModelAssembler<Endereco, EntityModel<Endereco>> {

    @Override
    @NonNull
    public EntityModel<Endereco> toModel(@NonNull Endereco endereco) {
        Long id = endereco.getId();
        return EntityModel.of(endereco,
                linkTo(methodOn(EnderecoController.class).buscar(id)).withSelfRel(),
                linkTo(methodOn(EnderecoController.class).atualizar(id, null)).withRel("update"),
                linkTo(methodOn(EnderecoController.class).remover(id)).withRel("delete"),
                linkTo(methodOn(EnderecoController.class).listar(Pageable.unpaged())).withRel("enderecos"));
    }
}
