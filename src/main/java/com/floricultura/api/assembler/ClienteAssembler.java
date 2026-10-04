package com.floricultura.api.assembler;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import com.floricultura.api.controller.ClienteController;
import com.floricultura.api.controller.EnderecoController;
import com.floricultura.api.controller.PedidoController;
import com.floricultura.api.entity.Cliente;

// monta os links HATEOAS de um cliente
@Component
public class ClienteAssembler implements RepresentationModelAssembler<Cliente, EntityModel<Cliente>> {

    @Override
    @NonNull
    public EntityModel<Cliente> toModel(@NonNull Cliente cliente) {
        Long id = cliente.getId();
        EntityModel<Cliente> model = EntityModel.of(cliente,
                linkTo(methodOn(ClienteController.class).buscar(id)).withSelfRel(),
                linkTo(methodOn(ClienteController.class).atualizar(id, null)).withRel("update"),
                linkTo(methodOn(ClienteController.class).remover(id)).withRel("delete"),
                linkTo(methodOn(ClienteController.class).listar(Pageable.unpaged())).withRel("clientes"),
                // nav pros pedidos desse cliente
                linkTo(methodOn(PedidoController.class)
                        .buscar(null, id, Pageable.unpaged())).withRel("pedidos"));
        // nav pro endereco do cliente, se tiver
        if (cliente.getEndereco() != null) {
            model.add(linkTo(methodOn(EnderecoController.class)
                    .buscar(cliente.getEndereco().getId())).withRel("endereco"));
        }
        return model;
    }
}
