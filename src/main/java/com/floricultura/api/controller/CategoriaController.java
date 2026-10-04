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

import com.floricultura.api.assembler.CategoriaAssembler;
import com.floricultura.api.dto.CategoriaRequest;
import com.floricultura.api.entity.Categoria;
import com.floricultura.api.service.CategoriaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

// endpoints das categorias
@RestController
@RequestMapping("/categorias")
@Tag(name = "Categorias", description = "Gerenciamento das categorias de produtos")
public class CategoriaController {

    private final CategoriaService categoriaService;
    private final CategoriaAssembler assembler;
    private final PagedResourcesAssembler<Categoria> pagedAssembler;

    public CategoriaController(CategoriaService categoriaService, CategoriaAssembler assembler,
            PagedResourcesAssembler<Categoria> pagedAssembler) {
        this.categoriaService = categoriaService;
        this.assembler = assembler;
        this.pagedAssembler = pagedAssembler;
    }

    @PostMapping
    @Operation(summary = "Cria uma nova categoria")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Categoria criada"),
            @ApiResponse(responseCode = "409", description = "Ja existe categoria com esse nome"),
            @ApiResponse(responseCode = "422", description = "Dados invalidos")
    })
    public ResponseEntity<EntityModel<Categoria>> criar(@Valid @RequestBody CategoriaRequest req) {
        Categoria salva = categoriaService.create(req);
        EntityModel<Categoria> model = assembler.toModel(salva);
        URI location = model.getRequiredLink("self").toUri();
        return ResponseEntity.created(location).body(model);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca uma categoria pelo id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categoria encontrada"),
            @ApiResponse(responseCode = "404", description = "Categoria nao encontrada")
    })
    public EntityModel<Categoria> buscar(@PathVariable Long id) {
        return assembler.toModel(categoriaService.findById(id));
    }

    @GetMapping
    @Operation(summary = "Lista as categorias de forma paginada")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Lista paginada de categorias"))
    public PagedModel<EntityModel<Categoria>> listar(Pageable pageable) {
        Page<Categoria> page = categoriaService.findAll(pageable);
        return pagedAssembler.toModel(page, assembler);
    }

    @GetMapping("/search")
    @Operation(summary = "Busca categorias por nome (consulta personalizada)")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Lista paginada filtrada por nome"))
    public PagedModel<EntityModel<Categoria>> buscarPorNome(@RequestParam String nome, Pageable pageable) {
        Page<Categoria> page = categoriaService.search(nome, pageable);
        return pagedAssembler.toModel(page, assembler);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza uma categoria existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categoria atualizada"),
            @ApiResponse(responseCode = "404", description = "Categoria nao encontrada"),
            @ApiResponse(responseCode = "409", description = "Nome ja usado por outra categoria"),
            @ApiResponse(responseCode = "422", description = "Dados invalidos")
    })
    public EntityModel<Categoria> atualizar(@PathVariable Long id, @Valid @RequestBody CategoriaRequest req) {
        return assembler.toModel(categoriaService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove uma categoria")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Categoria removida"),
            @ApiResponse(responseCode = "404", description = "Categoria nao encontrada"),
            @ApiResponse(responseCode = "409", description = "Categoria ainda tem produtos")
    })
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        categoriaService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
