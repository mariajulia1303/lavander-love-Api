package com.floricultura.api.assembler;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import com.floricultura.api.controller.CategoriaController;
import com.floricultura.api.controller.ProdutoController;
import com.floricultura.api.entity.Categoria;

// monta os links HATEOAS de uma categoria
@Component
public class CategoriaAssembler implements RepresentationModelAssembler<Categoria, EntityModel<Categoria>> {

    @Override
    @NonNull
    public EntityModel<Categoria> toModel(@NonNull Categoria categoria) {
        Long id = categoria.getId();
        return EntityModel.of(categoria,
                linkTo(methodOn(CategoriaController.class).buscar(id)).withSelfRel(),
                linkTo(methodOn(CategoriaController.class).atualizar(id, null)).withRel("update"),
                linkTo(methodOn(CategoriaController.class).remover(id)).withRel("delete"),
                linkTo(methodOn(CategoriaController.class).listar(Pageable.unpaged())).withRel("categorias"),
                // nav pros produtos dessa categoria
                linkTo(methodOn(ProdutoController.class)
                        .buscar(id, null, null, null, Pageable.unpaged())).withRel("produtos"));
    }
}
