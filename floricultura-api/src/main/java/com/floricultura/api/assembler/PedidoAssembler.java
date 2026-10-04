package com.floricultura.api.assembler;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import com.floricultura.api.controller.ClienteController;
import com.floricultura.api.controller.PedidoController;
import com.floricultura.api.controller.ProdutoController;
import com.floricultura.api.entity.Pedido;
import com.floricultura.api.entity.Produto;

// monta os links HATEOAS de um pedido
@Component
public class PedidoAssembler implements RepresentationModelAssembler<Pedido, EntityModel<Pedido>> {

    @Override
    @NonNull
    public EntityModel<Pedido> toModel(@NonNull Pedido pedido) {
        Long id = pedido.getId();
        EntityModel<Pedido> model = EntityModel.of(pedido,
                linkTo(methodOn(PedidoController.class).buscar(id)).withSelfRel(),
                linkTo(methodOn(PedidoController.class).atualizar(id, null)).withRel("update"),
                linkTo(methodOn(PedidoController.class).remover(id)).withRel("delete"),
                linkTo(methodOn(PedidoController.class).listar(Pageable.unpaged())).withRel("pedidos"));
        // nav pro cliente dono do pedido
        if (pedido.getCliente() != null) {
            model.add(linkTo(methodOn(ClienteController.class)
                    .buscar(pedido.getCliente().getId())).withRel("cliente"));
        }
        // nav pra cada produto do pedido
        if (pedido.getProdutos() != null) {
            for (Produto produto : pedido.getProdutos()) {
                model.add(linkTo(methodOn(ProdutoController.class)
                        .buscar(produto.getId())).withRel("produtos"));
            }
        }
        return model;
    }
}
