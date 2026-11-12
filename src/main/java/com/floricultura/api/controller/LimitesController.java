package com.floricultura.api.controller;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.Arrays;
import java.util.List;

import org.springframework.hateoas.EntityModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.floricultura.api.dto.LimitesResponse;
import com.floricultura.api.entity.Plano;
import com.floricultura.api.ratelimit.RateLimitService;
import com.floricultura.api.ratelimit.TokenBucket;
import com.floricultura.api.security.ApiKeyResolver;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

// endpoint publico que documenta o rate limit "ao vivo": quais sao os limites e quanto o cliente ja gastou
@RestController
@RequestMapping("/limites")
@Tag(name = "Rate limit", description = "Limites de uso da API (token bucket por plano, por IP e global)")
public class LimitesController {

    private final RateLimitService service;

    public LimitesController(RateLimitService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Mostra a politica de rate limit e o seu consumo atual",
            description = "Publico. Lista os limites de cada plano, do acesso anonimo (por IP) e o limite global, "
                    + "e mostra quantas requisicoes voce ainda pode fazer agora. "
                    + "Esta chamada tambem conta no seu limite.")
    @ApiResponse(responseCode = "200", description = "Politica e status atual")
    public EntityModel<LimitesResponse> limites(@Parameter(hidden = true) HttpServletRequest request) {
        List<LimitesResponse.Faixa> planos = Arrays.stream(Plano.values())
                .map(p -> new LimitesResponse.Faixa(p.name(), p.getCapacidade(), p.getRecargaPorMinuto()))
                .toList();

        RateLimitService.Identidade id = service.identificar(request, ApiKeyResolver.atual(request).orElse(null));
        TokenBucket.Resultado agora = service.espiar(id);

        LimitesResponse resposta = new LimitesResponse(
                "TOKEN_BUCKET",
                planos,
                new LimitesResponse.Faixa("ANONIMO", service.getAnonimoCapacidade(), service.getAnonimoRecarga()),
                new LimitesResponse.Faixa("GLOBAL", service.getGlobalCapacidade(), service.getGlobalRecarga()),
                new LimitesResponse.Penalidade(service.getPenalidadeTentativas(), service.getPenalidadeSegundos(),
                        "Quem continua enviando requisicoes depois de receber 429 (" + service.getPenalidadeTentativas()
                                + " negadas seguidas) fica bloqueado por " + service.getPenalidadeSegundos() + "s."),
                new LimitesResponse.Status(id.descricao(), id.politica(), id.capacidade(), agora.restantes(),
                        agora.resetSegundos(), agora.penalizado()));

        return EntityModel.of(resposta,
                linkTo(methodOn(LimitesController.class).limites(null)).withSelfRel(),
                linkTo(methodOn(ApiKeyController.class).minhaChave(null)).withRel("minhaChave"));
    }
}
