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

import com.floricultura.api.assembler.EnderecoAssembler;
import com.floricultura.api.dto.EnderecoRequest;
import com.floricultura.api.entity.Endereco;
import com.floricultura.api.service.EnderecoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

// endpoints dos enderecos
@RestController
@RequestMapping("/enderecos")
@Tag(name = "Enderecos", description = "Gerenciamento dos enderecos dos clientes")
public class EnderecoController {

    private final EnderecoService enderecoService;
    private final EnderecoAssembler assembler;
    private final PagedResourcesAssembler<Endereco> pagedAssembler;

    public EnderecoController(EnderecoService enderecoService, EnderecoAssembler assembler,
            PagedResourcesAssembler<Endereco> pagedAssembler) {
        this.enderecoService = enderecoService;
        this.assembler = assembler;
        this.pagedAssembler = pagedAssembler;
    }

    @PostMapping
    @Operation(summary = "Cria um novo endereco")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Endereco criado"),
            @ApiResponse(responseCode = "422", description = "Dados invalidos")
    })
    public ResponseEntity<EntityModel<Endereco>> criar(@Valid @RequestBody EnderecoRequest req) {
        Endereco salvo = enderecoService.create(req);
        EntityModel<Endereco> model = assembler.toModel(salvo);
        URI location = model.getRequiredLink("self").toUri();
        return ResponseEntity.created(location).body(model);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um endereco pelo id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Endereco encontrado"),
            @ApiResponse(responseCode = "404", description = "Endereco nao encontrado")
    })
    public EntityModel<Endereco> buscar(@PathVariable Long id) {
        return assembler.toModel(enderecoService.findById(id));
    }

    @GetMapping
    @Operation(summary = "Lista os enderecos de forma paginada")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Lista paginada de enderecos"))
    public PagedModel<EntityModel<Endereco>> listar(Pageable pageable) {
        Page<Endereco> page = enderecoService.findAll(pageable);
        return pagedAssembler.toModel(page, assembler);
    }

    @GetMapping("/search")
    @Operation(summary = "Busca enderecos por cidade (consulta personalizada)")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Lista paginada filtrada por cidade"))
    public PagedModel<EntityModel<Endereco>> buscarPorCidade(@RequestParam String cidade, Pageable pageable) {
        Page<Endereco> page = enderecoService.findByCidade(cidade, pageable);
        return pagedAssembler.toModel(page, assembler);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um endereco existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Endereco atualizado"),
            @ApiResponse(responseCode = "404", description = "Endereco nao encontrado"),
            @ApiResponse(responseCode = "422", description = "Dados invalidos")
    })
    public EntityModel<Endereco> atualizar(@PathVariable Long id, @Valid @RequestBody EnderecoRequest req) {
        return assembler.toModel(enderecoService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove um endereco")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Endereco removido"),
            @ApiResponse(responseCode = "404", description = "Endereco nao encontrado")
    })
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        enderecoService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
