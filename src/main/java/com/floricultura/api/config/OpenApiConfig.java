package com.floricultura.api.config;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.floricultura.api.entity.NivelAcesso;
import com.floricultura.api.exception.ApiError;
import com.floricultura.api.security.PoliticaAcesso;
import com.floricultura.api.versioning.VersaoApi;
import com.floricultura.api.web.ApiPaths;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

// obs: a tabela de limites da descricao reflete os valores padrao (Plano.java e application.properties).
// config do swagger: cabecalho da doc + tudo da parte 2 (chave de API, idempotencia,
// rate limit e versionamento) documentado automaticamente em todos os endpoints.
//
// a doc e dividida em dois grupos, um por versao da API:
//   /v3/api-docs/v1  -> versao 1 (padrao)
//   /v3/api-docs/v2  -> versao 2 (header X-API-Version: 2)
// no Swagger UI da pra trocar no seletor "Select a definition" la em cima.
@Configuration
public class OpenApiConfig {

    public static final String SECURITY_SCHEME = "ApiKeyAuth";
    private static final String REF_ERRO = "#/components/schemas/ApiError";

    @Bean
    public OpenAPI floriculturaOpenAPI() {
        return new OpenAPI()
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name("X-API-Key")
                        .description("Chave de API. Chaves de demonstracao: `ll_admin_dev_123` (ADMIN), "
                                + "`ll_escrita_dev_123` (ESCRITA) e `ll_leitura_dev_123` (LEITURA).")))
                .info(new Info()
                        .title("Lavender Love")
                        .description("API RESTful da Lavander Love, uma floricultura. Projeto final do curso de APIs com Spring Boot. "
                                + "Representa o dia a dia de uma loja de flores: clientes, endereços, produtos, categorias e pedidos.\n\n"

                                + "**Regras gerais**\n\n"
                                + "- JSON em **UTF-8**; mensagens de erro em português (pt-BR).\n"
                                + "- Datas no formato ISO 8601 e valores em reais (BRL).\n"
                                + "- Erros: 400 requisição mal formada, 401 sem chave, 403 nível insuficiente, "
                                + "404 não encontrado, 409 conflito, 422 validação, 429 limite excedido.\n\n"

                                + "**Autenticação (X-API-Key)**\n\n"
                                + "- Clique em **Authorize** e informe a chave. Níveis: `LEITURA` < `ESCRITA` < `ADMIN`.\n"
                                + "- GET de produtos e categorias é público. GET dos demais exige LEITURA, "
                                + "POST/PUT exigem ESCRITA, DELETE e /api-keys exigem ADMIN.\n\n"

                                + "**Conta e chave pessoal**\n\n"
                                + "- Não tem chave? Crie uma conta em `POST /auth/cadastro`, entre em `POST /auth/login` "
                                + "(recebe um `token`) e gere a sua chave em `POST /auth/minha-chave` com o header "
                                + "`X-Session-Token`. Cada pessoa tem **uma** chave (nível ESCRITA, plano BASICO).\n\n"

                                + "**Idempotência (X-Idempotency-Key)**\n\n"
                                + "- Todo POST exige o header `X-Idempotency-Key` (ex.: um UUID). Repetir a mesma chave com o "
                                + "mesmo corpo devolve a resposta original sem criar nada de novo (`X-Idempotency-Replayed: true`).\n\n"

                                + "**Rate limit (token bucket)**\n\n"
                                + "Cada cliente tem um *balde* de fichas: cada requisição gasta 1 ficha e o balde é "
                                + "reabastecido aos poucos. Dá pra fazer uma rajada até o tamanho do balde; depois, "
                                + "só no ritmo da recarga. Sem chave o limite é por **IP**; com chave, pelo **plano** da chave.\n\n"
                                + "| Quem | Capacidade (de uma vez) | Recarga |\n"
                                + "|---|---|---|\n"
                                + "| Sem chave (por IP) | 5 | 15 por minuto (1 a cada 4 s) |\n"
                                + "| Plano GRATUITO | 8 | 20 por minuto (1 a cada 3 s) |\n"
                                + "| Plano BASICO | 20 | 60 por minuto (1 por segundo) |\n"
                                + "| Plano PROFISSIONAL | 50 | 240 por minuto (4 por segundo) |\n"
                                + "| Global (API inteira) | 200 | 600 por minuto |\n\n"
                                + "- As chaves de demonstração são do plano GRATUITO.\n"
                                + "- Toda resposta traz `X-RateLimit-Limit` (capacidade), `X-RateLimit-Remaining` (fichas restantes), "
                                + "`X-RateLimit-Reset` (segundos até o balde encher) e `X-RateLimit-Policy` (plano e recarga).\n"
                                + "- Balde vazio: **429 Too Many Requests** com `Retry-After` (segundos até a próxima ficha).\n"
                                + "- **Penalidade:** continuar enviando depois do 429 (5 negadas seguidas) bloqueia o cliente por "
                                + "**30 s**, mesmo que o balde recarregue. Abuso repetido pode levar à desativação da chave.\n"
                                + "- `GET /limites` mostra essa política e quanto você já consumiu.\n\n"

                                + "**Versionamento (X-API-Version)**\n\n"
                                + "- Sem header = versão 1. `X-API-Version: 2` muda o formato de `GET /produtos` e "
                                + "`GET /produtos/{id}`. Troque o grupo no topo da página para ver a documentação de cada versão.\n\n"

                                + "**Termos de uso (resumo)**\n\n"
                                + "- Envie dados verdadeiros e respeite a privacidade (LGPD).\n"
                                + "- Evite excesso de requisições, tentativas de invasão e conteúdo ilegal: "
                                + "o acesso pode ser **bloqueado**.\n"
                                + "- Prefira cancelar pedidos a apagá-los: exclusões não podem ser desfeitas.\n"
                                + "- Banco em memória: os dados são apagados ao reiniciar.")
                        .version("2.1.0")
                        .termsOfService("https://github.com/mariajulia1303/lavander-love-Api#termos-de-servi%C3%A7o")
                        .contact(new Contact()
                                .name("Lavander Love")
                                .email("contato@lavanderlove.com.br"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://github.com/mariajulia1303/lavander-love-Api#licen%C3%A7a")));
    }

    @Bean
    public GroupedOpenApi grupoV1() {
        return GroupedOpenApi.builder()
                .group("v1")
                .pathsToMatch("/**")
                // na v1 ficam de fora os metodos marcados como versao 2
                .addOpenApiMethodFilter(m -> versaoDe(m) != 2)
                .addOpenApiCustomizer(customizer("1"))
                .build();
    }

    @Bean
    public GroupedOpenApi grupoV2() {
        return GroupedOpenApi.builder()
                .group("v2")
                .pathsToMatch("/**")
                // na v2 ficam de fora os metodos que foram substituidos (marcados como versao 1)
                .addOpenApiMethodFilter(m -> versaoDe(m) != 1)
                .addOpenApiCustomizer(customizer("2"))
                .build();
    }

    private static int versaoDe(Method method) {
        VersaoApi v = method.getAnnotation(VersaoApi.class);
        return v == null ? 0 : v.value();
    }

    // percorre todas as operacoes e adiciona: headers, cadeado de seguranca e respostas 400/401/403/429
    private OpenApiCustomizer customizer(String versao) {
        return openApi -> {
            if (openApi.getComponents() == null) {
                openApi.setComponents(new Components());
            }
            @SuppressWarnings("rawtypes")
            Map<String, Schema> schemasErro = ModelConverters.getInstance().readAll(ApiError.class);
            schemasErro.forEach(openApi.getComponents()::addSchemas);
            if (openApi.getComponents().getSecuritySchemes() == null
                    || !openApi.getComponents().getSecuritySchemes().containsKey(SECURITY_SCHEME)) {
                openApi.getComponents().addSecuritySchemes(SECURITY_SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.HEADER).name("X-API-Key"));
            }
            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().forEach((path, item) -> item.readOperationsMap()
                    .forEach((httpMethod, op) -> documentar(path, httpMethod, op, versao)));
        };
    }

    private void documentar(String path, PathItem.HttpMethod httpMethod, Operation op, String versao) {
        String metodo = httpMethod.name();
        if (op.getResponses() == null) {
            op.setResponses(new ApiResponses());
        }

        // X-API-Version em todas. no grupo v2 ele e obrigatorio e ja vem preenchido com 2,
        // assim o "Try it out" do Swagger manda o header e a resposta sai mesmo no formato v2.
        adicionarHeader(op, "X-API-Version", "2".equals(versao),
                "Versao da API (1 ou 2). Sem o header = 1.", versao, List.of("1", "2"));

        // seguranca conforme a mesma politica usada no filtro
        NivelAcesso nivel = PoliticaAcesso.nivelExigido(metodo, path);
        if (nivel != null) {
            op.addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME));
            acrescentarDescricao(op, "🔒 **Requer `X-API-Key` com nivel " + nivel + " ou superior.**");
            adicionarResposta(op, "401", "Chave de API ausente, invalida ou desativada");
            adicionarResposta(op, "403", "A chave nao tem nivel suficiente (exige " + nivel + ")");
        } else {
            acrescentarDescricao(op, "🌐 Endpoint publico (nao precisa de chave).");
        }

        // idempotencia em todo POST
        if (httpMethod == PathItem.HttpMethod.POST && !ApiPaths.comecaCom(path, ApiPaths.CONTA)) {
            adicionarHeader(op, "X-Idempotency-Key", true,
                    "Chave unica da operacao (ex: um UUID). Repetir a mesma chave com o mesmo corpo devolve a "
                            + "resposta original sem processar de novo.",
                    "3f6c2a1e-8b4d-4e2f-9a7c-1d2e3f4a5b6c", null);
            acrescentarDescricao(op, "🔁 Idempotente via `X-Idempotency-Key`: mesma chave + mesmo corpo = mesma "
                    + "resposta (header `X-Idempotency-Replayed: true`); mesma chave + corpo diferente = 422.");
            adicionarResposta(op, "400", "Header X-Idempotency-Key ausente ou JSON mal formado");
            adicionarResposta(op, "422", "Dados invalidos ou X-Idempotency-Key reutilizada com outro corpo");
        }

        // rate limit em todas
        ApiResponse r429 = respostaErro("Limite de requisicoes excedido (balde vazio) ou bloqueio temporario "
                + "por insistir depois do 429. Veja GET /limites.");
        r429.addHeaderObject("Retry-After", new Header()
                .description("Segundos ate poder tentar de novo").schema(new IntegerSchema()));
        r429.addHeaderObject("X-RateLimit-Limit", new Header()
                .description("Capacidade do balde do cliente").schema(new IntegerSchema()));
        r429.addHeaderObject("X-RateLimit-Remaining", new Header()
                .description("Fichas restantes (0 quando negado)").schema(new IntegerSchema()));
        r429.addHeaderObject("X-RateLimit-Reset", new Header()
                .description("Segundos ate o balde encher de novo").schema(new IntegerSchema()));
        op.getResponses().putIfAbsent("429", r429);
    }

    private void adicionarHeader(Operation op, String nome, boolean obrigatorio, String descricao,
            String exemplo, List<String> valores) {
        // se o springdoc ja criou esse header (ex: pelo headers = "X-API-Version=2" do mapeamento),
        // troca pelo nosso, que tem descricao, valor padrao e lista de opcoes
        if (op.getParameters() != null) {
            op.getParameters().removeIf(x -> nome.equalsIgnoreCase(x.getName()) && "header".equalsIgnoreCase(x.getIn()));
        }
        StringSchema schema = new StringSchema();
        if (valores != null) {
            schema.setEnum(valores);
            schema.setDefault(exemplo);
        }
        Parameter p = new HeaderParameter().name(nome).required(obrigatorio).description(descricao)
                .schema(schema).example(exemplo);
        op.addParametersItem(p);
    }

    private void adicionarResposta(Operation op, String codigo, String descricao) {
        op.getResponses().putIfAbsent(codigo, respostaErro(descricao));
    }

    private ApiResponse respostaErro(String descricao) {
        return new ApiResponse().description(descricao).content(new Content()
                .addMediaType("application/json", new MediaType().schema(new Schema<>().$ref(REF_ERRO))));
    }

    private void acrescentarDescricao(Operation op, String texto) {
        String atual = op.getDescription();
        if (atual != null && atual.contains(texto)) {
            return;
        }
        op.setDescription(atual == null || atual.isBlank() ? texto : atual + "\n\n" + texto);
    }
}
