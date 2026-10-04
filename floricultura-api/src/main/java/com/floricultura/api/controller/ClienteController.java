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

import com.floricultura.api.assembler.ClienteAssembler;
import com.floricultura.api.dto.ClienteRequest;
import com.floricultura.api.entity.Cliente;
import com.floricultura.api.service.ClienteService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

// endpoints dos clientes
@RestController
@RequestMapping("/clientes")
@Tag(name = "Clientes", description = "Gerenciamento dos clientes da floricultura")
public class ClienteController {

    private final ClienteService clienteService;
    private final ClienteAssembler assembler;
    private final PagedResourcesAssembler<Cliente> pagedAssembler;

    public ClienteController(ClienteService clienteService, ClienteAssembler assembler,
            PagedResourcesAssembler<Cliente> pagedAssembler) {
        this.clienteService = clienteService;
        this.assembler = assembler;
        this.pagedAssembler = pagedAssembler;
    }

    @PostMapping
    @Operation(summary = "Cria um novo cliente")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cliente criado"),
            @ApiResponse(responseCode = "409", description = "Ja existe cliente com esse email"),
            @ApiResponse(responseCode = "422", description = "Dados invalidos")
    })
    public ResponseEntity<EntityModel<Cliente>> criar(@Valid @RequestBody ClienteRequest req) {
        Cliente salvo = clienteService.create(req);
        EntityModel<Cliente> model = assembler.toModel(salvo);
        URI location = model.getRequiredLink("self").toUri();
        return ResponseEntity.created(location).body(model);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um cliente pelo id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cliente encontrado"),
            @ApiResponse(responseCode = "404", description = "Cliente nao encontrado")
    })
    public EntityModel<Cliente> buscar(@PathVariable Long id) {
        return assembler.toModel(clienteService.findById(id));
    }

    @GetMapping
    @Operation(summary = "Lista os clientes de forma paginada")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Lista paginada de clientes"))
    public PagedModel<EntityModel<Cliente>> listar(Pageable pageable) {
        Page<Cliente> page = clienteService.findAll(pageable);
        return pagedAssembler.toModel(page, assembler);
    }

    @GetMapping("/search")
    @Operation(summary = "Busca clientes por nome (consulta personalizada)")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Lista paginada filtrada por nome"))
    public PagedModel<EntityModel<Cliente>> buscarPorNome(@RequestParam String nome, Pageable pageable) {
        Page<Cliente> page = clienteService.search(nome, pageable);
        return pagedAssembler.toModel(page, assembler);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um cliente existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cliente atualizado"),
            @ApiResponse(responseCode = "404", description = "Cliente nao encontrado"),
            @ApiResponse(responseCode = "409", description = "Email ja usado por outro cliente"),
            @ApiResponse(responseCode = "422", description = "Dados invalidos")
    })
    public EntityModel<Cliente> atualizar(@PathVariable Long id, @Valid @RequestBody ClienteRequest req) {
        return assembler.toModel(clienteService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove um cliente")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Cliente removido"),
            @ApiResponse(responseCode = "404", description = "Cliente nao encontrado")
    })
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        clienteService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
