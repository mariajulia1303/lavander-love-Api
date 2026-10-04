package com.floricultura.api.controller;

import java.net.URI;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.floricultura.api.assembler.PedidoAssembler;
import com.floricultura.api.dto.PedidoRequest;
import com.floricultura.api.entity.Pedido;
import com.floricultura.api.entity.StatusPedido;
import com.floricultura.api.service.PedidoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;

// endpoints dos pedidos
@RestController
@RequestMapping("/pedidos")
@Tag(name = "Pedidos", description = "Gerenciamento dos pedidos da floricultura")
public class PedidoController {

    private final PedidoService pedidoService;
    private final PedidoAssembler assembler;
    private final PagedResourcesAssembler<Pedido> pagedAssembler;

    public PedidoController(PedidoService pedidoService, PedidoAssembler assembler,
            PagedResourcesAssembler<Pedido> pagedAssembler) {
        this.pedidoService = pedidoService;
        this.assembler = assembler;
        this.pagedAssembler = pagedAssembler;
    }

    @PostMapping
    @Operation(summary = "Cria um novo pedido")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pedido criado"),
            @ApiResponse(responseCode = "409", description = "Cliente ou produto informado nao existe"),
            @ApiResponse(responseCode = "422", description = "Dados invalidos")
    })
    public ResponseEntity<EntityModel<Pedido>> criar(@Valid @RequestBody PedidoRequest req) {
        Pedido salvo = pedidoService.create(req);
        EntityModel<Pedido> model = assembler.toModel(salvo);
        URI location = model.getRequiredLink("self").toUri();
        return ResponseEntity.created(location).body(model);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um pedido pelo id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedido encontrado"),
            @ApiResponse(responseCode = "404", description = "Pedido nao encontrado")
    })
    public EntityModel<Pedido> buscar(@PathVariable Long id) {
        return assembler.toModel(pedidoService.findById(id));
    }

    @GetMapping
    @Operation(summary = "Lista os pedidos de forma paginada")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Lista paginada de pedidos"))
    public PagedModel<EntityModel<Pedido>> listar(Pageable pageable) {
        Page<Pedido> page = pedidoService.findAll(pageable);
        return pagedAssembler.toModel(page, assembler);
    }

    @GetMapping("/search")
    @Operation(summary = "Busca pedidos por status ou cliente (consulta personalizada)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista paginada filtrada"),
            @ApiResponse(responseCode = "422", description = "Nenhum filtro informado")
    })
    public PagedModel<EntityModel<Pedido>> buscar(
            @RequestParam(required = false) StatusPedido status,
            @RequestParam(required = false) Long clienteId,
            Pageable pageable) {
        Page<Pedido> page;
        if (status != null) {
            page = pedidoService.findByStatus(status, pageable);
        } else if (clienteId != null) {
            page = pedidoService.findByCliente(clienteId, pageable);
        } else {
            // mesma ideia do produto: /search sem filtro nenhum nao rola, pede pelo menos um -> 422
            throw new ConstraintViolationException(
                    "Informe ao menos um filtro: status ou clienteId", java.util.Collections.emptySet());
        }
        return pagedAssembler.toModel(page, assembler);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um pedido existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedido atualizado"),
            @ApiResponse(responseCode = "404", description = "Pedido nao encontrado"),
            @ApiResponse(responseCode = "409", description = "Cliente ou produto informado nao existe"),
            @ApiResponse(responseCode = "422", description = "Dados invalidos")
    })
    public EntityModel<Pedido> atualizar(@PathVariable Long id, @Valid @RequestBody PedidoRequest req) {
        return assembler.toModel(pedidoService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove um pedido")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Pedido removido"),
            @ApiResponse(responseCode = "404", description = "Pedido nao encontrado")
    })
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        pedidoService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
