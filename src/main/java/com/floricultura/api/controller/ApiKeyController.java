package com.floricultura.api.controller;

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

import com.floricultura.api.assembler.ApiKeyAssembler;
import com.floricultura.api.dto.ApiKeyCriadaResponse;
import com.floricultura.api.dto.ApiKeyRequest;
import com.floricultura.api.dto.ApiKeyUpdateRequest;
import com.floricultura.api.entity.ApiKey;
import com.floricultura.api.entity.NivelAcesso;
import com.floricultura.api.exception.ResourceNotFoundException;
import com.floricultura.api.security.ApiKeyResolver;
import com.floricultura.api.service.ApiKeyService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;

// geracao e gerenciamento das chaves de API (tudo aqui exige chave ADMIN, menos o /me)
@RestController
@RequestMapping("/api-keys")
@Tag(name = "Chaves de API", description = "Geracao e gerenciamento das chaves usadas no header X-API-Key. "
        + "Exige chave de nivel ADMIN (exceto GET /api-keys/me).")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;
    private final ApiKeyAssembler assembler;
    private final PagedResourcesAssembler<ApiKey> pagedAssembler;

    public ApiKeyController(ApiKeyService apiKeyService, ApiKeyAssembler assembler,
            PagedResourcesAssembler<ApiKey> pagedAssembler) {
        this.apiKeyService = apiKeyService;
        this.assembler = assembler;
        this.pagedAssembler = pagedAssembler;
    }

    @PostMapping
    @Operation(summary = "Gera uma nova chave de API",
            description = "Cria uma chave com o nivel informado (LEITURA, ESCRITA ou ADMIN) e o plano de uso "
                    + "(GRATUITO, BASICO ou PROFISSIONAL, que define o rate limit; padrao GRATUITO). "
                    + "O valor da chave (campo `chave`) aparece SOMENTE nesta resposta: no banco fica apenas o hash.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Chave gerada"),
            @ApiResponse(responseCode = "422", description = "Dados invalidos")
    })
    public ResponseEntity<EntityModel<ApiKeyCriadaResponse>> gerar(@Valid @RequestBody ApiKeyRequest req) {
        EntityModel<ApiKeyCriadaResponse> model = assembler.toModel(apiKeyService.gerar(req));
        URI location = model.getRequiredLink("self").toUri();
        return ResponseEntity.created(location).body(model);
    }

    @GetMapping("/me")
    @Operation(summary = "Mostra a chave usada na requisicao",
            description = "Qualquer chave valida pode chamar. Util pro front-end conferir a chave e o nivel de acesso.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dados da chave atual"),
            @ApiResponse(responseCode = "401", description = "Chave ausente ou invalida")
    })
    public EntityModel<ApiKey> minhaChave(@Parameter(hidden = true) HttpServletRequest request) {
        ApiKey atual = ApiKeyResolver.atual(request)
                .orElseThrow(() -> new ResourceNotFoundException("Nenhuma chave de API na requisicao"));
        return assembler.toModel(atual);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca uma chave pelo id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Chave encontrada"),
            @ApiResponse(responseCode = "404", description = "Chave nao encontrada")
    })
    public EntityModel<ApiKey> buscar(@PathVariable Long id) {
        return assembler.toModel(apiKeyService.findById(id));
    }

    @GetMapping
    @Operation(summary = "Lista as chaves de forma paginada", description = "O valor das chaves nunca e exibido, so o prefixo.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Lista paginada de chaves"))
    public PagedModel<EntityModel<ApiKey>> listar(@ParameterObject Pageable pageable) {
        Page<ApiKey> page = apiKeyService.findAll(pageable);
        return pagedAssembler.toModel(page, assembler);
    }

    @GetMapping("/search")
    @Operation(summary = "Busca chaves por nivel e/ou situacao (consulta personalizada)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista paginada filtrada"),
            @ApiResponse(responseCode = "422", description = "Nenhum filtro informado")
    })
    public PagedModel<EntityModel<ApiKey>> pesquisar(
            @RequestParam(required = false) NivelAcesso nivel,
            @RequestParam(required = false) Boolean ativa,
            @ParameterObject Pageable pageable) {
        if (nivel == null && ativa == null) {
            throw new ConstraintViolationException("Informe ao menos um filtro: nivel ou ativa",
                    java.util.Collections.emptySet());
        }
        return pagedAssembler.toModel(apiKeyService.search(nivel, ativa, pageable), assembler);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza nome, nivel, plano ou situacao de uma chave",
            description = "Use `ativa: false` pra suspender uma chave sem apaga-la (ex: cliente abusando da API). "
                    + "Trocar o `plano` muda o rate limit da chave na hora.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Chave atualizada"),
            @ApiResponse(responseCode = "404", description = "Chave nao encontrada"),
            @ApiResponse(responseCode = "409", description = "Tentativa de remover a ultima chave ADMIN ativa"),
            @ApiResponse(responseCode = "422", description = "Dados invalidos")
    })
    public EntityModel<ApiKey> atualizar(@PathVariable Long id, @Valid @RequestBody ApiKeyUpdateRequest req,
            @Parameter(hidden = true) HttpServletRequest request) {
        ApiKey quemPediu = request == null ? null : ApiKeyResolver.atual(request).orElse(null);
        return assembler.toModel(apiKeyService.update(id, req, quemPediu));
    }

    @PostMapping("/{id}/rotacionar")
    @Operation(summary = "Gera um novo valor para a chave (rotacao)",
            description = "O valor antigo para de funcionar na hora. O novo aparece somente nesta resposta.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Chave rotacionada"),
            @ApiResponse(responseCode = "404", description = "Chave nao encontrada")
    })
    public EntityModel<ApiKeyCriadaResponse> rotacionar(@PathVariable Long id) {
        return assembler.toModel(apiKeyService.rotacionar(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Revoga (apaga) uma chave")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Chave revogada"),
            @ApiResponse(responseCode = "404", description = "Chave nao encontrada"),
            @ApiResponse(responseCode = "409", description = "Nao da pra revogar a propria chave nem a ultima ADMIN")
    })
    public ResponseEntity<Void> revogar(@PathVariable Long id, @Parameter(hidden = true) HttpServletRequest request) {
        ApiKey quemPediu = request == null ? null : ApiKeyResolver.atual(request).orElse(null);
        apiKeyService.revogar(id, quemPediu);
        return ResponseEntity.noContent().build();
    }
}
