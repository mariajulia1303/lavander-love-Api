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
import com.floricultura.api.dto.ProdutoV2Response;
import com.floricultura.api.entity.Produto;

// links HATEOAS do produto na versao 2 (mesmos links da v1, corpo no formato novo)
@Component
public class ProdutoV2Assembler implements RepresentationModelAssembler<Produto, EntityModel<ProdutoV2Response>> {

    @Override
    @NonNull
    public EntityModel<ProdutoV2Response> toModel(@NonNull Produto produto) {
        Long id = produto.getId();
        EntityModel<ProdutoV2Response> model = EntityModel.of(ProdutoV2Response.from(produto),
                linkTo(methodOn(ProdutoController.class).buscarV2(id)).withSelfRel(),
                linkTo(methodOn(ProdutoController.class).atualizar(id, null)).withRel("update"),
                linkTo(methodOn(ProdutoController.class).remover(id)).withRel("delete"),
                linkTo(methodOn(ProdutoController.class).listarV2(null, null, null, null, null, Pageable.unpaged())).withRel("produtos"));
        if (produto.getCategoria() != null) {
            model.add(linkTo(methodOn(CategoriaController.class)
                    .buscar(produto.getCategoria().getId())).withRel("categoria"));
        }
        return model;
    }
}
