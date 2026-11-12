package com.floricultura.api.controller;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.floricultura.api.dto.ApiKeyCriadaResponse;
import com.floricultura.api.dto.CadastroRequest;
import com.floricultura.api.dto.LoginRequest;
import com.floricultura.api.dto.LoginResponse;
import com.floricultura.api.dto.UsuarioResponse;
import com.floricultura.api.security.SessaoService;
import com.floricultura.api.service.UsuarioService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

// "area da conta": cadastro, login e a chave de API pessoal.
// fluxo: POST /auth/cadastro -> POST /auth/login (recebe o token) -> POST /auth/minha-chave com o header
// X-Session-Token -> usa a chave recebida no header X-API-Key no resto da API.
// estas rotas nao pedem X-API-Key nem X-Idempotency-Key, mas contam no rate limit (por IP),
// o que tambem protege o login contra quem tenta adivinhar senha no chute.
@RestController
@RequestMapping("/auth")
@Tag(name = "Conta e login", description = "Cadastro, login e geracao da chave de API pessoal "
        + "(uma por pessoa). Nao precisa de X-API-Key: depois do login use o header X-Session-Token.")
public class AuthController {

    private final UsuarioService usuarioService;

    public AuthController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping("/cadastro")
    @Operation(summary = "Cria uma conta",
            description = "Publico. A senha e guardada so como hash (PBKDF2). Depois faca POST /auth/login.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Conta criada"),
            @ApiResponse(responseCode = "409", description = "Ja existe conta com este e-mail"),
            @ApiResponse(responseCode = "422", description = "Dados invalidos")
    })
    public ResponseEntity<EntityModel<UsuarioResponse>> cadastrar(@Valid @RequestBody CadastroRequest req) {
        EntityModel<UsuarioResponse> model = EntityModel.of(usuarioService.cadastrar(req),
                linkTo(methodOn(AuthController.class).login(null)).withRel("login"));
        return ResponseEntity.status(HttpStatus.CREATED).body(model);
    }

    @PostMapping("/login")
    @Operation(summary = "Entra na conta",
            description = "Publico. Devolve um `token` de sessao: envie no header `X-Session-Token` em "
                    + "GET /auth/me e POST /auth/minha-chave. E-mail ou senha errados = 401.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login feito"),
            @ApiResponse(responseCode = "401", description = "E-mail ou senha incorretos"),
            @ApiResponse(responseCode = "422", description = "Dados invalidos")
    })
    public EntityModel<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        return EntityModel.of(usuarioService.login(req), linksDaConta());
    }

    @GetMapping("/me")
    @Operation(summary = "Mostra a minha conta e a situacao da minha chave")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dados da conta"),
            @ApiResponse(responseCode = "401", description = "Sessao ausente ou expirada")
    })
    public EntityModel<UsuarioResponse> minhaConta(
            @Parameter(description = "Token recebido no login")
            @RequestHeader(value = SessaoService.HEADER, required = false) String token) {
        return EntityModel.of(usuarioService.minhaConta(token), linksDaConta());
    }

    @PostMapping("/minha-chave")
    @Operation(summary = "Gera a minha chave de API pessoal",
            description = "Cada conta tem UMA chave (nivel ESCRITA, plano BASICO). Se ja existir, gera um valor "
                    + "novo para ela e o antigo para de funcionar. O valor (campo `chave`) aparece so nesta resposta.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Chave gerada"),
            @ApiResponse(responseCode = "401", description = "Sessao ausente ou expirada"),
            @ApiResponse(responseCode = "409", description = "Chave desativada por um administrador")
    })
    public ResponseEntity<EntityModel<ApiKeyCriadaResponse>> gerarMinhaChave(
            @Parameter(description = "Token recebido no login")
            @RequestHeader(value = SessaoService.HEADER, required = false) String token) {
        EntityModel<ApiKeyCriadaResponse> model = EntityModel.of(usuarioService.gerarMinhaChave(token),
                linksDaConta());
        return ResponseEntity.status(HttpStatus.CREATED).body(model);
    }

    @PostMapping("/logout")
    @Operation(summary = "Sai da conta (o token deixa de valer)")
    @ApiResponse(responseCode = "204", description = "Sessao encerrada")
    public ResponseEntity<Void> logout(
            @RequestHeader(value = SessaoService.HEADER, required = false) String token) {
        usuarioService.logout(token);
        return ResponseEntity.noContent().build();
    }

    private Link[] linksDaConta() {
        return new Link[] {
                linkTo(methodOn(AuthController.class).minhaConta(null)).withRel("minhaConta"),
                linkTo(methodOn(AuthController.class).gerarMinhaChave(null)).withRel("gerarChave"),
                linkTo(methodOn(AuthController.class).logout(null)).withRel("logout")
        };
    }
}
