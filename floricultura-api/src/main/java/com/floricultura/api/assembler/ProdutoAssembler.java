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
import com.floricultura.api.entity.Produto;

// monta os links HATEOAS de um produto
@Component
public class ProdutoAssembler implements RepresentationModelAssembler<Produto, EntityModel<Produto>> {

    @Override
    @NonNull
    public EntityModel<Produto> toModel(@NonNull Produto produto) {
        Long id = produto.getId();
        EntityModel<Produto> model = EntityModel.of(produto,
                linkTo(methodOn(ProdutoController.class).buscar(id)).withSelfRel(),
                linkTo(methodOn(ProdutoController.class).atualizar(id, null)).withRel("update"),
                linkTo(methodOn(ProdutoController.class).remover(id)).withRel("delete"),
                linkTo(methodOn(ProdutoController.class).listar(Pageable.unpaged())).withRel("produtos"));
        // nav pra categoria do produto, se tiver
        if (produto.getCategoria() != null) {
            model.add(linkTo(methodOn(CategoriaController.class)
                    .buscar(produto.getCategoria().getId())).withRel("categoria"));
        }
        return model;
    }
}
