package com.floricultura.api.controller;

import java.math.BigDecimal;
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

import com.floricultura.api.assembler.ProdutoAssembler;
import com.floricultura.api.dto.ProdutoRequest;
import com.floricultura.api.entity.Produto;
import com.floricultura.api.service.ProdutoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;

// endpoints dos produtos
@RestController
@RequestMapping("/produtos")
@Tag(name = "Produtos", description = "Gerenciamento dos produtos da floricultura")
public class ProdutoController {

    private final ProdutoService produtoService;
    private final ProdutoAssembler assembler;
    private final PagedResourcesAssembler<Produto> pagedAssembler;

    public ProdutoController(ProdutoService produtoService, ProdutoAssembler assembler,
            PagedResourcesAssembler<Produto> pagedAssembler) {
        this.produtoService = produtoService;
        this.assembler = assembler;
        this.pagedAssembler = pagedAssembler;
    }

    @PostMapping
    @Operation(summary = "Cria um novo produto")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Produto criado"),
            @ApiResponse(responseCode = "409", description = "Categoria informada nao existe"),
            @ApiResponse(responseCode = "422", description = "Dados invalidos")
    })
    public ResponseEntity<EntityModel<Produto>> criar(@Valid @RequestBody ProdutoRequest req) {
        Produto salvo = produtoService.create(req);
        EntityModel<Produto> model = assembler.toModel(salvo);
        URI location = model.getRequiredLink("self").toUri();
        return ResponseEntity.created(location).body(model);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um produto pelo id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Produto encontrado"),
            @ApiResponse(responseCode = "404", description = "Produto nao encontrado")
    })
    public EntityModel<Produto> buscar(@PathVariable Long id) {
        return assembler.toModel(produtoService.findById(id));
    }

    @GetMapping
    @Operation(summary = "Lista os produtos de forma paginada")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Lista paginada de produtos"))
    public PagedModel<EntityModel<Produto>> listar(Pageable pageable) {
        Page<Produto> page = produtoService.findAll(pageable);
        return pagedAssembler.toModel(page, assembler);
    }

    @GetMapping("/search")
    @Operation(summary = "Busca produtos por categoria ou faixa de preco (consulta personalizada)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista paginada filtrada"),
            @ApiResponse(responseCode = "422", description = "Nenhum filtro informado")
    })
    public PagedModel<EntityModel<Produto>> buscar(
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) BigDecimal precoMin,
            @RequestParam(required = false) BigDecimal precoMax,
            @RequestParam(required = false) String nome,
            Pageable pageable) {
        Page<Produto> page;
        if (categoriaId != null) {
            page = produtoService.findByCategoria(categoriaId, pageable);
        } else if (precoMin != null && precoMax != null) {
            page = produtoService.findByFaixaPreco(precoMin, precoMax, pageable);
        } else if (nome != null) {
            page = produtoService.search(nome, pageable);
        } else {
            // sem filtro nenhum nao faz sentido o /search virar um findAll disfarcado,
            // entao a gente avisa que precisa mandar pelo menos um parametro -> 422
            throw new ConstraintViolationException(
                    "Informe ao menos um filtro: categoriaId, precoMin+precoMax ou nome",
                    java.util.Collections.emptySet());
        }
        return pagedAssembler.toModel(page, assembler);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um produto existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Produto atualizado"),
            @ApiResponse(responseCode = "404", description = "Produto nao encontrado"),
            @ApiResponse(responseCode = "409", description = "Categoria informada nao existe"),
            @ApiResponse(responseCode = "422", description = "Dados invalidos")
    })
    public EntityModel<Produto> atualizar(@PathVariable Long id, @Valid @RequestBody ProdutoRequest req) {
        return assembler.toModel(produtoService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove um produto")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Produto removido"),
            @ApiResponse(responseCode = "404", description = "Produto nao encontrado")
    })
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        produtoService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
