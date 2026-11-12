package com.floricultura.api.controller;

import java.math.BigDecimal;
import java.net.URI;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springdoc.core.annotations.ParameterObject;
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
import com.floricultura.api.assembler.ProdutoV2Assembler;
import com.floricultura.api.dto.ProdutoRequest;
import com.floricultura.api.dto.ProdutoV2Response;
import com.floricultura.api.entity.Produto;
import com.floricultura.api.service.ProdutoService;
import com.floricultura.api.versioning.VersaoApi;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
    private final ProdutoV2Assembler assemblerV2;
    private final PagedResourcesAssembler<Produto> pagedAssembler;

    public ProdutoController(ProdutoService produtoService, ProdutoAssembler assembler,
            ProdutoV2Assembler assemblerV2, PagedResourcesAssembler<Produto> pagedAssembler) {
        this.produtoService = produtoService;
        this.assembler = assembler;
        this.assemblerV2 = assemblerV2;
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

    // ---------------- VERSAO 1 (padrao: sem header ou X-API-Version: 1) ----------------

    @GetMapping("/{id}")
    @VersaoApi(1)
    @Operation(summary = "Busca um produto pelo id (v1)",
            description = "Versao 1 (padrao). Devolve o produto com a categoria completa.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Produto encontrado"),
            @ApiResponse(responseCode = "404", description = "Produto nao encontrado")
    })
    public EntityModel<Produto> buscar(@PathVariable Long id) {
        return assembler.toModel(produtoService.findById(id));
    }

    @GetMapping
    @VersaoApi(1)
    @Operation(summary = "Lista os produtos de forma paginada (v1)",
            description = "Versao 1 (padrao). Itens em `_embedded.produtoList`.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Lista paginada de produtos"))
    public PagedModel<EntityModel<Produto>> listar(@ParameterObject Pageable pageable) {
        Page<Produto> page = produtoService.findAll(pageable);
        return pagedAssembler.toModel(page, assembler);
    }

    // ---------------- VERSAO 2 (header X-API-Version: 2) ----------------
    // o "headers" no mapeamento faz o Spring escolher este metodo quando o header vier com 2.

    @GetMapping(value = "/{id}", headers = "X-API-Version=2")
    @VersaoApi(2)
    @Operation(summary = "Busca um produto pelo id (v2)",
            description = "Versao 2 (envie `X-API-Version: 2`). Mudancas: categoria resumida (id e nome), "
                    + "novos campos `precoFormatado` (ex: \"R$ 89,90\") e `disponivel` (tem estoque?).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Produto encontrado (formato v2)"),
            @ApiResponse(responseCode = "404", description = "Produto nao encontrado")
    })
    public EntityModel<ProdutoV2Response> buscarV2(@PathVariable Long id) {
        return assemblerV2.toModel(produtoService.findById(id));
    }

    @GetMapping(headers = "X-API-Version=2")
    @VersaoApi(2)
    @Operation(summary = "Lista os produtos de forma paginada, com filtros (v2)",
            description = "Versao 2 (envie `X-API-Version: 2`). Itens em `_embedded.produtos`, no formato ProdutoV2. "
                    + "Novidade da v2: filtros opcionais e combinaveis direto na listagem "
                    + "(`nome`, `categoriaId`, `precoMin`, `precoMax`, `disponivel`). "
                    + "Ex.: `?categoriaId=1&precoMax=100&disponivel=true&sort=preco,asc`.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista paginada de produtos (formato v2)"),
            @ApiResponse(responseCode = "422", description = "Filtro invalido (ex: precoMin maior que precoMax)")
    })
    public PagedModel<EntityModel<ProdutoV2Response>> listarV2(
            @Parameter(description = "Parte do nome (sem diferenciar maiusculas)", example = "rosa")
            @RequestParam(required = false) String nome,
            @Parameter(description = "Id da categoria", example = "1")
            @RequestParam(required = false) Long categoriaId,
            @Parameter(description = "Preco minimo", example = "20.00")
            @RequestParam(required = false) BigDecimal precoMin,
            @Parameter(description = "Preco maximo", example = "150.00")
            @RequestParam(required = false) BigDecimal precoMax,
            @Parameter(description = "true = so com estoque; false = so esgotados")
            @RequestParam(required = false) Boolean disponivel,
            @ParameterObject Pageable pageable) {
        if ((precoMin != null && precoMin.signum() < 0) || (precoMax != null && precoMax.signum() < 0)) {
            throw new ConstraintViolationException("precoMin e precoMax nao podem ser negativos",
                    java.util.Collections.emptySet());
        }
        if (precoMin != null && precoMax != null && precoMin.compareTo(precoMax) > 0) {
            throw new ConstraintViolationException("precoMin nao pode ser maior que precoMax",
                    java.util.Collections.emptySet());
        }
        Page<Produto> page = produtoService.filtrar(nome, categoriaId, precoMin, precoMax, disponivel, pageable);
        return pagedAssembler.toModel(page, assemblerV2);
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
            @ParameterObject Pageable pageable) {
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
