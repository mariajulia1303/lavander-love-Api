package com.floricultura.api.config;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.floricultura.api.entity.NivelAcesso;
import com.floricultura.api.exception.ApiError;
import com.floricultura.api.security.PoliticaAcesso;
import com.floricultura.api.security.SessaoService;
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
    // segundo cadeado do "Authorize": o token de login (rotas /auth/me, /auth/minha-chave e /auth/logout)
    public static final String SESSAO_SCHEME = "LoginToken";
    private static final String REF_ERRO = "#/components/schemas/ApiError";


    // descricao geral que aparece no topo do Swagger (markdown)
    private static final String DESCRICAO = """
            API RESTful da **Lavander Love**, uma floricultura. Projeto final do curso de APIs com Spring Boot.
            Representa o dia a dia de uma loja de flores: **clientes**, **endereços**, **produtos**, **categorias** e \
            **pedidos**, com contas de usuário e chaves de API para controlar o acesso.

            ### Como testar em 5 passos
            1. Crie uma conta em **Conta e login → `POST /auth/cadastro`** e entre em **`POST /auth/login`**. Copie o `token`.
            2. Clique em **Authorize** e cole o token no campo **LoginToken**.
            3. Gere a sua chave em **`POST /auth/minha-chave`** e copie o campo `chave`.
            4. Em **Authorize**, cole a chave no campo **ApiKeyAuth**. Também dá para usar uma chave de demonstração: \
            `ll_admin_dev_123` (ADMIN), `ll_escrita_dev_123` (ESCRITA) ou `ll_leitura_dev_123` (LEITURA).
            5. Abra uma rota, clique em **Try it out** e depois em **Execute**.

            ### Abas da documentação
            | Aba | O que faz |
            |---|---|
            | **Conta e login** | Cadastro, login e chave de API pessoal de cada pessoa |
            | **Produtos** | Catálogo da loja (buquês, plantas e vasos). Tem **versão 2** |
            | **Categorias** | Grupos de produtos (Buques, Plantas, Vasos) |
            | **Clientes** | Quem compra na loja, com o endereço de entrega |
            | **Enderecos** | Endereços de entrega dos clientes |
            | **Pedidos** | Compras dos clientes, com produtos e status |
            | **Chaves de API** | Gerar e gerenciar chaves (só ADMIN) |
            | **Rate limit** | Limites de uso de cada plano e quanto você já consumiu |

            ### Dados que já vêm cadastrados
            | Recurso | Registros |
            |---|---|
            | Categorias | `1` Buques · `2` Plantas · `3` Vasos |
            | Produtos | `1` Buque de Rosas Vermelhas (R$ 89,90) · `2` Suculenta Echeveria (R$ 19,90) · `3` Vaso de Ceramica Branco (R$ 34,50) |
            | Clientes | `1` Maria Silva (maria.silva@email.com, 11999990000) |
            | Endereços | `1` Rua das Flores, 100, Centro, Sao Paulo/SP, CEP 01001000 |

            ### Regras gerais
            - JSON em **UTF-8**; mensagens de erro em português.
            - Datas no formato ISO 8601 (ex.: `2026-11-17T14:30:00`) e valores em reais (ex.: `89.90`).
            - Listagens são **paginadas**: `page` (começa em 0), `size` (itens por página) e `sort` (ex.: `nome,asc`).
            - Respostas com **HATEOAS**: cada registro traz `_links` para navegar (ex.: `self`, `update`, `delete`).

            ### Códigos de status
            | Código | Quando acontece |
            |---|---|
            | **200** OK | Consulta ou alteração feita com sucesso |
            | **201** Created | Registro criado (o header `Location` traz o endereço dele) |
            | **204** No Content | Registro excluído ou logout feito (resposta sem corpo) |
            | **400** Bad Request | JSON mal formado, parâmetro com tipo errado ou `X-Idempotency-Key` ausente |
            | **401** Unauthorized | Sem chave de API, chave inválida, e-mail/senha incorretos ou sessão expirada |
            | **403** Forbidden | A chave não tem o nível exigido pela operação |
            | **404** Not Found | O registro procurado (id) não existe |
            | **405** Method Not Allowed | O método HTTP não existe naquela rota |
            | **409** Conflict | Conflito com dados existentes: e-mail repetido, id relacionado inexistente ou registro em uso |
            | **415** Unsupported Media Type | O corpo não foi enviado como `application/json` |
            | **422** Unprocessable Entity | Campos inválidos, filtro/ordenação inválidos ou `X-Idempotency-Key` reutilizada com outro corpo |
            | **429** Too Many Requests | Limite de requisições excedido (veja o header `Retry-After`) |
            | **500** Internal Server Error | Erro inesperado no servidor |

            Todo erro tem o mesmo formato:
            ```json
            { "timestamp": "2026-11-17T14:30:00Z", "status": 404, "error": "Not Found", "message": "Categoria 99 nao encontrada" }
            ```
            Nos erros de validação (422) vem também `fieldErrors`, com o campo e o motivo.

            ### Autenticação (X-API-Key)
            - Clique em **Authorize** e informe a chave. Níveis: `LEITURA` < `ESCRITA` < `ADMIN`.
            - GET de produtos, categorias e limites é **público**. GET dos demais exige LEITURA, POST/PUT exigem ESCRITA \
            e DELETE exige ADMIN.
            - **Gerenciar chaves:** aba **Chaves de API** (`/api-keys`), só com chave ADMIN: gerar (com nível e plano), \
            listar, alterar nível/plano, ativar/desativar, rotacionar e excluir.

            ### Conta e chave pessoal (X-Session-Token)
            - O login devolve um `token` que vale **60 minutos**. No Swagger ele vai no **Authorize**, campo **LoginToken**.
            - Cada pessoa tem **uma** chave pessoal (nível ESCRITA, plano BASICO): gerar em `POST /auth/minha-chave` \
            e excluir em `DELETE /auth/minha-chave`.
            - A senha é guardada só como **hash** (PBKDF2) e nunca volta nas respostas.

            ### Idempotência (X-Idempotency-Key)
            - Todo POST (fora da aba Conta e login) exige o header `X-Idempotency-Key`, por exemplo um UUID.
            - Mesma chave + mesmo corpo: devolve a resposta original sem criar nada de novo \
            (header `X-Idempotency-Replayed: true`).
            - Mesma chave + corpo diferente: **422**. Sem o header: **400**. As chaves valem por 24 horas.

            ### Rate limit (token bucket)
            Cada cliente tem um *balde* de fichas: cada requisição gasta 1 ficha e o balde é reabastecido aos poucos. \
            Sem chave o limite é por **IP**; com chave, pelo **plano** da chave.

            | Quem | Capacidade (de uma vez) | Recarga |
            |---|---|---|
            | Sem chave (por IP) | 5 | 15 por minuto (1 a cada 4 s) |
            | Plano GRATUITO | 8 | 20 por minuto (1 a cada 3 s) |
            | Plano BASICO | 20 | 60 por minuto (1 por segundo) |
            | Plano PROFISSIONAL | 50 | 240 por minuto (4 por segundo) |
            | Global (API inteira) | 200 | 600 por minuto |

            - As chaves de demonstração são do plano GRATUITO; as chaves pessoais, do BASICO.
            - Toda resposta traz `X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset` e `X-RateLimit-Policy`.
            - Balde vazio: **429** com `Retry-After`. Insistir depois do 429 (5 negadas seguidas) bloqueia por **30 s**.
            - `GET /limites` mostra essa política e quanto você já consumiu.

            ### Versionamento (X-API-Version)
            - Sem header = **versão 1**. Com `X-API-Version: 2`, `GET /produtos` e `GET /produtos/{id}` mudam de formato: \
            trazem `precoFormatado` (ex.: "R$ 89,90"), `disponivel` e a categoria resumida, e a listagem ganha filtros \
            combinados (`nome`, `categoriaId`, `precoMin`, `precoMax`, `disponivel`).
            - Troque a versão no seletor **Select a definition**, no topo da página.

            ### CORS
            - O navegador só deixa um front-end de outro endereço ler as respostas se a API liberar. Liberados por padrão: \
            Live Server (`:5500`), servidor do IntelliJ (`:63342`) e `http://localhost:8080`. Em produção a lista vem \
            da variável de ambiente `LAVANDER_CORS_ORIGINS`.
            - Métodos: `GET`, `POST`, `PUT`, `DELETE`, `OPTIONS`. Headers aceitos: `Content-Type`, `Accept`, `X-API-Key`, \
            `X-API-Version`, `X-Idempotency-Key` e `X-Session-Token`.
            - O Swagger não precisa de CORS, porque roda no mesmo endereço da API.

            ### Termos de uso (resumo)
            - Envie dados verdadeiros e respeite a privacidade (LGPD).
            - Excesso de requisições, tentativas de invasão e conteúdo ilegal podem levar ao **bloqueio** do acesso.
            - Prefira cancelar pedidos a apagá-los: exclusões não podem ser desfeitas.
            """;

    // descricao de cada aba (tag) do Swagger, com exemplos do que ela faz
    private static final Map<String, String> ABAS = new java.util.LinkedHashMap<>();
    static {
        ABAS.put("Conta e login", """
                Crie a sua conta e gere a sua **chave de API pessoal**. **Exemplos:** `POST /auth/cadastro` cria a conta; \
                `POST /auth/login` devolve o `token` (cole no Authorize, campo **LoginToken**); \
                `POST /auth/minha-chave` gera a chave `ll_...`; `DELETE /auth/minha-chave` exclui; \
                `GET /auth/me` mostra os seus dados; `POST /auth/logout` encerra a sessão.""");
        ABAS.put("Produtos", """
                Catálogo da floricultura. **Exemplos:** `GET /produtos` lista tudo; `GET /produtos/1` traz o \
                *Buque de Rosas Vermelhas*; `GET /produtos/search?precoMin=20&precoMax=100` filtra por preço; \
                `POST /produtos` cadastra um produto novo numa categoria. Na **versão 2** (`X-API-Version: 2`) \
                a resposta traz `precoFormatado` e `disponivel`, e a listagem aceita filtros combinados.""");
        ABAS.put("Categorias", """
                Grupos de produtos. **Exemplos:** `GET /categorias` lista *Buques*, *Plantas* e *Vasos*; \
                `GET /categorias/search?nome=buq` busca pelo nome; `POST /categorias` cria uma categoria nova \
                (o nome não pode repetir); `DELETE /categorias/3` só funciona se nenhum produto usar a categoria.""");
        ABAS.put("Clientes", """
                Quem compra na loja. São dados pessoais, então **todas as rotas exigem chave** (LEITURA para ver). \
                **Exemplos:** `GET /clientes/1` traz a *Maria Silva*; `GET /clientes/search?nome=maria` busca pelo nome; \
                `POST /clientes` cadastra um cliente já com o endereço (o e-mail não pode repetir).""");
        ABAS.put("Enderecos", """
                Endereços de entrega dos clientes. **Exemplos:** `GET /enderecos/1` traz *Rua das Flores, 100*; \
                `GET /enderecos/search?cidade=Sao Paulo` busca pela cidade; `PUT /enderecos/1` corrige o endereço.""");
        ABAS.put("Pedidos", """
                Compras dos clientes. Status: `PENDENTE` → `PAGO` → `EM_PREPARACAO` → `ENVIADO` → `ENTREGUE` \
                (ou `CANCELADO`). **Exemplos:** `POST /pedidos` com `clienteId: 1` e `produtoIds: [1, 2]` cria o pedido \
                da Maria Silva; `GET /pedidos/search?status=PENDENTE` lista os pendentes; `PUT /pedidos/1` muda o status.""");
        ABAS.put("Chaves de API", """
                Gerenciamento das chaves do header `X-API-Key`, **só com chave ADMIN**. **Exemplos:** `POST /api-keys` \
                gera uma chave com nível (`LEITURA`, `ESCRITA`, `ADMIN`) e plano (`GRATUITO`, `BASICO`, `PROFISSIONAL`); \
                `GET /api-keys` lista (só o prefixo, nunca o valor); `PUT /api-keys/1` troca o plano ou desativa; \
                `POST /api-keys/1/rotacionar` gera um valor novo; `DELETE /api-keys/1` exclui. \
                `GET /api-keys/me` (qualquer nível) mostra a chave que você está usando.""");
        ABAS.put("Rate limit", """
                Limites de uso (token bucket). **Exemplo:** `GET /limites` (público) mostra a capacidade e a recarga de \
                cada plano, o limite sem chave e o global, a penalidade e quantas fichas **você** ainda tem.""");
    }

    @Bean
    public OpenAPI floriculturaOpenAPI() {
        return new OpenAPI()
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name("X-API-Key")
                        .description("Chave de API. Chaves de demonstracao: `ll_admin_dev_123` (ADMIN), "
                                + "`ll_escrita_dev_123` (ESCRITA) e `ll_leitura_dev_123` (LEITURA)."))
                        .addSecuritySchemes(SESSAO_SCHEME, esquemaSessao()))
                .info(new Info()
                        .title("Lavender Love")
                        .description(DESCRICAO)
                        .version("2.1.1")
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
                .pathsToExclude(rotasOcultas())
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
                .pathsToExclude(rotasOcultas())
                // na v2 ficam de fora os metodos que foram substituidos (marcados como versao 1)
                .addOpenApiMethodFilter(m -> versaoDe(m) != 1)
                .addOpenApiCustomizer(customizer("2"))
                .build();
    }

    // o gerenciamento de chaves (/api-keys) e so pra administradores e lista as chaves de todo mundo,
    // por isso fica FORA da documentacao publica. os endpoints continuam funcionando normalmente
    // (com chave ADMIN). pra mostrar de novo: lavander.docs.mostrar-gerenciamento-chaves=true
    @Value("${lavander.docs.mostrar-gerenciamento-chaves:false}")
    private boolean mostrarGerenciamentoChaves;

    private String[] rotasOcultas() {
        return mostrarGerenciamentoChaves ? new String[] { "/nada-oculto" } : new String[] { "/api-keys/**", "/api-keys" };
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
            if (!openApi.getComponents().getSecuritySchemes().containsKey(SESSAO_SCHEME)) {
                openApi.getComponents().addSecuritySchemes(SESSAO_SCHEME, esquemaSessao());
            }
            formatarSchemas(openApi);
            descreverAbas(openApi);
            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().forEach((path, item) -> item.readOperationsMap()
                    .forEach((httpMethod, op) -> {
                        documentar(path, httpMethod, op, versao);
                        formatarParametros(path, op);
                    }));
        };
    }

    // ------------------------------------------------------------------
    // FORMATACAO DOS CAMPOS
    // deixa cada parametro e cada campo dos corpos com descricao clara,
    // exemplo e o limite de caracteres/valores escrito por extenso.
    // ------------------------------------------------------------------

    // nome do recurso no singular, pra montar "Id do produto", "Id da categoria"...
    private static final Map<String, String> RECURSOS = Map.of(
            "produtos", "do produto",
            "categorias", "da categoria",
            "clientes", "do cliente",
            "enderecos", "do endereço",
            "pedidos", "do pedido",
            "api-keys", "da chave de API");

    // limite do texto de busca "nome" em cada recurso (o mesmo do cadastro)
    private static final Map<String, Integer> LIMITE_NOME = Map.of(
            "produtos", 100,
            "categorias", 60,
            "clientes", 100);

    private static String recursoDe(String path) {
        String[] partes = path.split("/");
        return partes.length > 1 ? partes[1] : "";
    }

    private void formatarParametros(String path, Operation op) {
        if (op.getParameters() == null) {
            return;
        }
        String recurso = recursoDe(path);
        for (Parameter p : op.getParameters()) {
            @SuppressWarnings("rawtypes")
            Schema s = p.getSchema();
            if (s == null || p.getName() == null) {
                continue;
            }
            String nome = p.getName();
            // a descricao padrao do Pageable vem em ingles: troca pela nossa
            if ("page".equals(nome) || "size".equals(nome) || "sort".equals(nome)) {
                p.setDescription(null);
            }
            switch (nome) {
                case "id" -> {
                    s.setMinimum(java.math.BigDecimal.ONE);
                    p.setExample(1);
                    descrever(p, "Número de identificação " + RECURSOS.getOrDefault(recurso, "do registro") + ".",
                            "Número inteiro, a partir de 1.");
                }
                case "categoriaId", "clienteId" -> {
                    s.setMinimum(java.math.BigDecimal.ONE);
                    p.setExample(1);
                    descrever(p, "Número de identificação " + ("categoriaId".equals(nome) ? "da categoria" : "do cliente")
                            + ".", "Número inteiro, a partir de 1.");
                }
                case "nome", "cidade" -> {
                    int max = "cidade".equals(nome) ? 60 : LIMITE_NOME.getOrDefault(recurso, 100);
                    s.setMinLength(1);
                    s.setMaxLength(max);
                    if (p.getExample() == null) {
                        p.setExample("cidade".equals(nome) ? "Sao Paulo"
                                : "categorias".equals(recurso) ? "Buques"
                                : "clientes".equals(recurso) ? "Maria" : "rosa");
                    }
                    descrever(p, "cidade".equals(nome) ? "Nome da cidade (ou parte dele)."
                            : "Nome (ou parte dele). Não diferencia maiúsculas de minúsculas.",
                            "Texto de 1 a " + max + " caracteres.");
                }
                case "precoMin", "precoMax" -> {
                    s.setMinimum(java.math.BigDecimal.ZERO);
                    descrever(p, "precoMin".equals(nome) ? "Preço mínimo, em reais (R$)." : "Preço máximo, em reais (R$).",
                            "Valor a partir de 0, com ponto nas casas decimais (ex.: 20.00).");
                }
                case "disponivel" -> descrever(p, "Filtra pelo estoque.",
                        "`true` = só com estoque; `false` = só esgotados.");
                case "ativa" -> descrever(p, "Filtra pela situação da chave.",
                        "`true` = só ativas; `false` = só desativadas.");
                case "status" -> descrever(p, "Status do pedido.", "Escolha um dos valores da lista.");
                case "nivel" -> descrever(p, "Nível de acesso da chave.", "Escolha um dos valores da lista.");
                case "page" -> {
                    s.setMinimum(java.math.BigDecimal.ZERO);
                    descrever(p, "Número da página.", "Começa em 0 (0 = primeira página).");
                }
                case "size" -> {
                    s.setMinimum(java.math.BigDecimal.ONE);
                    descrever(p, "Quantidade de itens por página.", "Número inteiro, a partir de 1 (padrão: 20).");
                }
                case "sort" -> descrever(p, "Ordenação.",
                        "Formato `campo,asc` (crescente) ou `campo,desc` (decrescente). Ex.: `nome,asc`.");
                case "X-Idempotency-Key" -> {
                    s.setMinLength(1);
                    s.setMaxLength(100);
                    descrever(p, null, "Texto de 1 a 100 caracteres (um UUID tem 36).");
                }
                case "X-Session-Token" -> {
                    s.setMinLength(43);
                    s.setMaxLength(43);
                    descrever(p, "Token recebido em `POST /auth/login`.", "Texto de 43 caracteres.");
                }
                default -> {
                    // nada
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // EXEMPLOS: valores reais (os mesmos do data.sql) no lugar de "string" e 0
    // ------------------------------------------------------------------
    private static final String DATA = "2026-11-17T14:30:00";

    // exemplo por campo, valendo pra qualquer modelo
    private static final Map<String, Object> EXEMPLO_GERAL = Map.ofEntries(
            Map.entry("id", 1),
            Map.entry("email", "maria.silva@email.com"),
            Map.entry("telefone", "11999990000"),
            Map.entry("logradouro", "Rua das Flores"),
            Map.entry("numero", "100"),
            Map.entry("bairro", "Centro"),
            Map.entry("cidade", "Sao Paulo"),
            Map.entry("uf", "SP"),
            Map.entry("cep", "01001000"),
            Map.entry("preco", new java.math.BigDecimal("89.90")),
            Map.entry("estoque", 15),
            Map.entry("disponivel", true),
            Map.entry("categoriaId", 1),
            Map.entry("clienteId", 1),
            Map.entry("produtoIds", List.of(1, 2)),
            Map.entry("dataPedido", DATA),
            Map.entry("criadaEm", DATA),
            Map.entry("criadoEm", DATA),
            Map.entry("ultimoUso", DATA),
            Map.entry("expiraEm", "2026-11-17T15:30:00Z"),
            Map.entry("prefixo", "ll_3kq9X..."),
            Map.entry("ativa", true));

    // exemplo de campos com o mesmo nome em modelos diferentes (ex.: "nome")
    private static final Map<String, Map<String, Object>> EXEMPLO_POR_MODELO = Map.ofEntries(
            Map.entry("Categoria", ex("nome", "Buques", "descricao", "Buques variados pra qualquer ocasiao")),
            Map.entry("CategoriaResumo", ex("nome", "Buques")),
            Map.entry("Produto", ex("nome", "Buque de Rosas Vermelhas")),
            Map.entry("ProdutoV2", ex("nome", "Buque de Rosas Vermelhas")),
            Map.entry("Cliente", ex("nome", "Maria Silva")),
            Map.entry("Usuario", ex("nome", "Maria Silva")),
            Map.entry("ApiKey", ex("nome", "Chave inicial ADMIN", "prefixo", "ll_admin...")),
            Map.entry("ApiKeyCriada", ex("nome", "Front-end da loja")),
            Map.entry("ChaveResumo", ex("prefixo", "ll_3kq9X...")),
            Map.entry("ApiError", ex("timestamp", "2026-11-17T14:30:00Z", "status", 404, "error", "Not Found",
                    "message", "Categoria 99 nao encontrada")),
            Map.entry("FieldErrorItem", ex("field", "nome", "message", "deve ter entre 2 e 60 caracteres")),
            Map.entry("PageMetadata", ex("size", 20, "totalElements", 3, "totalPages", 1, "number", 0)),
            Map.entry("Link", ex("href", "http://localhost:8080/produtos/1", "templated", false)),
            Map.entry("Login", ex("token", "q3Z8kL0vN2xR7tY5wB1cD4eF6gH9jK3mP0sU2vX4zA8")));

    // monta um Map<String, Object> a partir de pares chave, valor (deixa os tipos explicitos pro compilador)
    private static Map<String, Object> ex(Object... chaveValor) {
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        for (int k = 0; k < chaveValor.length; k += 2) {
            m.put((String) chaveValor[k], chaveValor[k + 1]);
        }
        return m;
    }

    // recurso de cada modelo, pra montar o link de exemplo do _links
    private static final Map<String, String> RECURSO_DO_MODELO = Map.of(
            "Categoria", "categorias",
            "Produto", "produtos",
            "ProdutoV2", "produtos",
            "Cliente", "clientes",
            "Endereco", "enderecos",
            "Pedido", "pedidos",
            "ApiKey", "api-keys",
            "ApiKeyCriada", "api-keys",
            "Usuario", "auth/me");

    // "EntityModelCategoria" -> "Categoria", "ProdutoRequest" -> "Produto", "UsuarioResponse" -> "Usuario"
    private static String tipoDoModelo(String nomeSchema) {
        String n = nomeSchema.replace("PagedModel", "").replace("CollectionModel", "").replace("EntityModel", "");
        for (String sufixo : List.of("Request", "Response", "UpdateRequest")) {
            if (n.endsWith(sufixo) && n.length() > sufixo.length()) {
                n = n.substring(0, n.length() - sufixo.length());
            }
        }
        return n.replace("ApiKeyUpdate", "ApiKey");
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void colocarExemplos(String nomeSchema, Map<String, Schema> campos) {
        String tipo = tipoDoModelo(nomeSchema);
        // cadastro e login ficam com os campos vazios de proposito
        if (nomeSchema.equals("CadastroRequest") || nomeSchema.equals("LoginRequest")) {
            return;
        }
        Map<String, Object> doModelo = EXEMPLO_POR_MODELO.getOrDefault(tipo, Map.of());
        for (Map.Entry<String, Schema> e : new java.util.ArrayList<>(campos.entrySet())) {
            String nome = e.getKey();
            Schema campo = e.getValue();
            if ("_links".equals(nome)) {
                // links HATEOAS: mostra um "self" de verdade em vez de additionalProp1, 2, 3
                String recurso = RECURSO_DO_MODELO.get(tipo);
                String href = "http://localhost:8080/" + (recurso == null ? "" : recurso
                        + (recurso.contains("/") ? "" : nomeSchema.startsWith("PagedModel") ? "?page=0&size=20" : "/1"));
                // Schema generico (o ObjectSchema transformaria o exemplo em texto)
                Schema<Object> links = new Schema<>();
                links.setType("object");
                links.setDescription("Links para navegar pela API (HATEOAS)");
                links.setExample(Map.of("self", Map.of("href", href)));
                campos.put(nome, links);
                continue;
            }
            if (campo == null || campo.get$ref() != null || campo.getExample() != null) {
                continue;
            }
            Object exemplo = doModelo.containsKey(nome) ? doModelo.get(nome) : EXEMPLO_GERAL.get(nome);
            if (exemplo != null) {
                campo.setExample(exemplo);
            }
        }
    }

    // percorre os modelos (corpos de requisicao e resposta): exemplos + limites de cada campo
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void formatarSchemas(OpenAPI openApi) {
        if (openApi.getComponents() == null || openApi.getComponents().getSchemas() == null) {
            return;
        }
        for (Map.Entry<String, Schema> m : openApi.getComponents().getSchemas().entrySet()) {
            Schema modelo = m.getValue();
            Map<String, Schema> campos = modelo.getProperties();
            if (campos == null) {
                continue;
            }
            colocarExemplos(m.getKey(), campos);
            for (Schema campo : campos.values()) {
                String regra = regraDeTamanho(campo.getMinLength(), campo.getMaxLength());
                if (regra == null && campo.getMinimum() != null) {
                    String minimo = campo.getMinimum().stripTrailingZeros().toPlainString();
                    regra = Boolean.TRUE.equals(campo.getExclusiveMinimum())
                            ? "Valor maior que " + minimo + "."
                            : "Valor a partir de " + minimo + ".";
                }
                if (regra != null) {
                    String atual = campo.getDescription();
                    if (atual == null || !atual.contains(regra)) {
                        campo.setDescription(atual == null || atual.isBlank() ? regra : atual + " " + regra);
                    }
                }
            }
        }
    }

    private static String regraDeTamanho(Integer min, Integer max) {
        if (max == null && (min == null || min == 0)) {
            return null;
        }
        if (min != null && max != null && min.equals(max)) {
            return "Exatamente " + max + " caracteres.";
        }
        if (min != null && min > 0 && max != null) {
            return "De " + min + " a " + max + " caracteres.";
        }
        if (max != null) {
            return "Até " + max + " caracteres.";
        }
        return "No mínimo " + min + " caracteres.";
    }

    // monta "descricao **regra**" sem repetir quando o swagger e gerado de novo
    private static void descrever(Parameter p, String padrao, String regra) {
        String base = p.getDescription();
        if (base == null || base.isBlank()) {
            base = padrao == null ? "" : padrao;
        }
        String negrito = "**" + regra + "**";
        if (base.contains(negrito)) {
            return;
        }
        p.setDescription(base.isBlank() ? negrito : base + "\n\n" + negrito);
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

        // rotas da conta que pedem o token de login: em vez de um campo X-Session-Token em cada uma,
        // o token e informado uma vez so no botao "Authorize" (cadeado LoginToken)
        boolean usaSessao = op.getParameters() != null && op.getParameters()
                .removeIf(x -> SessaoService.HEADER.equalsIgnoreCase(x.getName()) && "header".equalsIgnoreCase(x.getIn()));
        if (usaSessao) {
            op.addSecurityItem(new SecurityRequirement().addList(SESSAO_SCHEME));
            acrescentarDescricao(op, "🔑 **Requer login:** clique em **Authorize** e cole o `token` recebido em "
                    + "`POST /auth/login` no campo **LoginToken (X-Session-Token)**.");
            if (!path.endsWith("/logout")) {
                adicionarResposta(op, "401", "Token de login ausente, invalido ou expirado");
            }
        }

        // seguranca conforme a mesma politica usada no filtro
        NivelAcesso nivel = PoliticaAcesso.nivelExigido(metodo, path);
        if (usaSessao) {
            // ja documentado acima
        } else if (nivel != null) {
            op.addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME));
            acrescentarDescricao(op, "🔒 **Requer `X-API-Key` com nivel " + nivel + " ou superior.**");
            adicionarResposta(op, "401", "Chave de API ausente, invalida ou desativada");
            if (nivel != NivelAcesso.LEITURA) {
                adicionarResposta(op, "403", "A chave nao tem nivel suficiente (exige " + nivel + ")");
            }
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

        completarStatus(path, httpMethod, op);
        padronizarRespostas(path, httpMethod, op, nivel, usaSessao);
    }

    // ------------------------------------------------------------------
    // STATUS CODES: completa cada operacao com todos os codigos que ela pode devolver
    // ------------------------------------------------------------------
    private static final Map<String, String> NAO_ENCONTRADO = Map.of(
            "produtos", "Produto 99 nao encontrado",
            "categorias", "Categoria 99 nao encontrada",
            "clientes", "Cliente 99 nao encontrado",
            "enderecos", "Endereco 99 nao encontrado",
            "pedidos", "Pedido 99 nao encontrado",
            "api-keys", "Chave de API 99 nao encontrada");

    // conflitos (409) reais de cada recurso: [descricao, mensagem de exemplo]
    private static final Map<String, String[]> CONFLITO_CRIAR = Map.of(
            "produtos", new String[] { "A categoria informada (categoriaId) nao existe", "Categoria 99 nao existe" },
            "categorias", new String[] { "Ja existe uma categoria com este nome", "Violacao de integridade dos dados" },
            "clientes", new String[] { "Ja existe um cliente com este e-mail",
                    "Ja existe um cliente com o email 'maria.silva@email.com'" },
            "pedidos", new String[] { "O cliente ou algum produto informado nao existe", "Cliente 99 nao existe" },
            "auth", new String[] { "Ja existe uma conta com este e-mail", "Ja existe uma conta com este e-mail." });
    private static final Map<String, String[]> CONFLITO_ALTERAR = Map.of(
            "produtos", CONFLITO_CRIAR.get("produtos"),
            "categorias", CONFLITO_CRIAR.get("categorias"),
            "clientes", CONFLITO_CRIAR.get("clientes"),
            "pedidos", CONFLITO_CRIAR.get("pedidos"),
            "api-keys", new String[] { "Nao e possivel rebaixar ou desativar a ultima chave ADMIN ativa",
                    "Nao e possivel rebaixar ou desativar a ultima chave ADMIN ativa" });
    private static final Map<String, String[]> CONFLITO_EXCLUIR = Map.of(
            "produtos", new String[] { "O produto faz parte de algum pedido", "Violacao de integridade dos dados" },
            "categorias", new String[] { "Ainda existem produtos nesta categoria", "Violacao de integridade dos dados" },
            "clientes", new String[] { "O cliente tem pedidos", "Violacao de integridade dos dados" },
            "enderecos", new String[] { "O endereco esta em uso por um cliente", "Violacao de integridade dos dados" },
            "api-keys", new String[] { "Nao e possivel excluir a propria chave em uso nem a ultima chave ADMIN ativa",
                    "Nao e possivel revogar a propria chave que esta sendo usada na requisicao" });

    // mensagem de quando o /search vem sem nenhum filtro (a mesma que os controllers devolvem)
    private static final Map<String, String> SEM_FILTRO = Map.of(
            "produtos", "Informe ao menos um filtro: categoriaId, precoMin+precoMax ou nome",
            "pedidos", "Informe ao menos um filtro: status ou clienteId",
            "api-keys", "Informe ao menos um filtro: nivel ou ativa");

    // adiciona o codigo (se ainda nao existir) ja com o corpo de erro e a mensagem de exemplo
    private void status(Operation op, String codigo, String descricao, String mensagem) {
        ApiResponse r = op.getResponses().get(codigo);
        if (r == null) {
            r = new ApiResponse().description(descricao);
            op.getResponses().addApiResponse(codigo, r);
        }
        if (!temExemplo(r)) {
            r.setContent(conteudoErro(Integer.parseInt(codigo), mensagem));
        }
    }

    private static boolean temExemplo(ApiResponse r) {
        return r.getContent() != null && r.getContent().get("application/json") != null
                && r.getContent().get("application/json").getExample() != null;
    }

    private static Content conteudoErro(int numero, String mensagem) {
        return new Content().addMediaType("application/json",
                new MediaType().schema(new Schema<>().$ref(REF_ERRO)).example(exemploErro(numero, mensagem)));
    }

    private void completarStatus(String path, PathItem.HttpMethod httpMethod, Operation op) {
        String recurso = recursoDe(path);
        boolean temId = path.contains("{id}");
        boolean temCorpo = op.getRequestBody() != null;
        boolean conta = "auth".equals(recurso);

        switch (httpMethod) {
            case GET -> {
                if (temId) {
                    status(op, "400", "O id informado nao e um numero", "Valor 'abc' invalido para o parametro 'id'.");
                    status(op, "404", "Registro nao encontrado", NAO_ENCONTRADO.getOrDefault(recurso, "Recurso nao encontrado"));
                } else if (!conta && !"limites".equals(recurso) && !path.endsWith("/me")) {
                    status(op, "400", "Parametro com tipo invalido (ex.: texto onde era numero)",
                            "Valor 'abc' invalido para o parametro 'page'.");
                    String semFiltro = path.endsWith("/search") ? SEM_FILTRO.get(recurso) : null;
                    status(op, "422", semFiltro != null
                            ? "Nenhum filtro informado ou campo de ordenacao (sort) inexistente"
                            : "Campo de ordenacao (sort) inexistente",
                            semFiltro != null ? semFiltro
                                    : "Campo de ordenacao invalido: 'preco2'. Use um campo real da entidade no formato "
                                            + "'campo,direcao' (ex: nome,asc).");
                }
            }
            case POST -> {
                if (temCorpo) {
                    status(op, "400", "JSON mal formado" + (conta ? "" : " ou header X-Idempotency-Key ausente"),
                            conta ? "Corpo da requisicao invalido ou mal formado. Confira o JSON e os tipos dos campos."
                                    : "Operacoes POST exigem o header X-Idempotency-Key (ex: um UUID gerado pelo cliente).");
                    status(op, "415", "O corpo nao foi enviado como application/json", "Envie o corpo como application/json.");
                    status(op, "422", "Campos invalidos (validacao)" + (conta ? "" : " ou X-Idempotency-Key reutilizada com outro corpo"),
                            "Falha na validacao dos campos");
                    String[] c = path.endsWith("/cadastro") ? CONFLITO_CRIAR.get("auth")
                            : conta ? null : CONFLITO_CRIAR.get(recurso);
                    if (c != null) {
                        status(op, "409", c[0], c[1]);
                    }
                }
                if (temId) {
                    status(op, "404", "Registro nao encontrado", NAO_ENCONTRADO.getOrDefault(recurso, "Recurso nao encontrado"));
                }
                if (path.endsWith("/login")) {
                    status(op, "401", "E-mail ou senha incorretos", "E-mail ou senha incorretos.");
                }
                if (path.endsWith("/minha-chave")) {
                    status(op, "409", "Chave desativada por um administrador",
                            "Sua chave foi desativada por um administrador e nao pode ser renovada.");
                }
            }
            case PUT -> {
                status(op, "400", "JSON mal formado ou id que nao e numero",
                        "Corpo da requisicao invalido ou mal formado. Confira o JSON e os tipos dos campos.");
                status(op, "404", "Registro nao encontrado", NAO_ENCONTRADO.getOrDefault(recurso, "Recurso nao encontrado"));
                status(op, "415", "O corpo nao foi enviado como application/json", "Envie o corpo como application/json.");
                status(op, "422", "Campos invalidos (validacao)", "Falha na validacao dos campos");
                String[] c = CONFLITO_ALTERAR.get(recurso);
                if (c != null) {
                    status(op, "409", c[0], c[1]);
                }
            }
            case DELETE -> {
                if (temId) {
                    status(op, "400", "O id informado nao e um numero", "Valor 'abc' invalido para o parametro 'id'.");
                    status(op, "404", "Registro nao encontrado", NAO_ENCONTRADO.getOrDefault(recurso, "Recurso nao encontrado"));
                }
                if (path.endsWith("/minha-chave")) {
                    status(op, "404", "A conta nao tem chave", "Voce ainda nao tem chave de API para excluir.");
                }
                String[] c = CONFLITO_EXCLUIR.get(recurso);
                if (c != null) {
                    status(op, "409", c[0], c[1]);
                }
            }
            default -> {
                // nada
            }
        }
        status(op, "500", "Erro inesperado no servidor", "Erro interno no servidor");
    }

    // ------------------------------------------------------------------
    // CORPO DAS RESPOSTAS: erros com o formato real (ApiError) + exemplo, sucesso como application/json
    // ------------------------------------------------------------------
    @SuppressWarnings("rawtypes")
    private void padronizarRespostas(String path, PathItem.HttpMethod httpMethod, Operation op, NivelAcesso nivel,
            boolean usaSessao) {
        for (Map.Entry<String, ApiResponse> e : op.getResponses().entrySet()) {
            String codigo = e.getKey();
            ApiResponse resp = e.getValue();
            int numero;
            try {
                numero = Integer.parseInt(codigo);
            } catch (NumberFormatException ex) {
                continue;
            }
            if (numero >= 400 && !temExemplo(resp)) {
                String msg = switch (numero) {
                        case 401 -> usaSessao
                                ? "Sessao ausente ou expirada. Faca login de novo e envie o token no header X-Session-Token."
                                : "Esta operacao exige uma chave de API. Envie o header X-API-Key (nivel minimo: "
                                        + (nivel == null ? NivelAcesso.LEITURA : nivel) + ").";
                        case 403 -> "Sua chave tem nivel " + (nivel == NivelAcesso.ADMIN ? "ESCRITA" : "LEITURA")
                                + ", mas esta operacao exige nivel " + nivel + ".";
                        case 429 -> "Limite de requisicoes excedido para Chave ll_admin... (plano GRATUITO: 8 de uma vez, "
                                + "recarga de 20 por minuto). Tente novamente em 3s.";
                        case 400 -> "Corpo da requisicao invalido ou mal formado. Confira o JSON e os tipos dos campos.";
                        case 404 -> "Recurso nao encontrado";
                        case 409 -> "Violacao de integridade dos dados";
                        case 415 -> "Envie o corpo como application/json.";
                        case 422 -> "Falha na validacao dos campos";
                        default -> "Erro interno no servidor";
                    };
                resp.setContent(conteudoErro(numero, msg));
                if (resp.getDescription() == null || resp.getDescription().isBlank()) {
                    resp.setDescription(org.springframework.http.HttpStatus.valueOf(numero).getReasonPhrase());
                }
            } else if (resp.getContent() != null && resp.getContent().containsKey("*/*")) {
                // "*/*" vira "application/json" (o formato que a API devolve de verdade)
                Content novo = new Content();
                resp.getContent().forEach((tipo, conteudo) -> novo.addMediaType("*/*".equals(tipo) ? "application/json" : tipo, conteudo));
                resp.setContent(novo);
            }
            if (numero == 201 && (resp.getHeaders() == null || !resp.getHeaders().containsKey("Location"))) {
                resp.addHeaderObject("Location", new Header()
                        .description("Endereco do registro criado (ex.: http://localhost:8080/categorias/4)")
                        .schema(new StringSchema()));
            }
        }
        // ordena os codigos (200, 201, 204, 400, 401...) pra ficar facil de ler
        ApiResponses ordenadas = new ApiResponses();
        op.getResponses().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> ordenadas.addApiResponse(e.getKey(), e.getValue()));
        op.setResponses(ordenadas);
    }

    private static Map<String, Object> exemploErro(int status, String mensagem) {
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("timestamp", "2026-11-17T14:30:00Z");
        m.put("status", status);
        m.put("error", org.springframework.http.HttpStatus.valueOf(status).getReasonPhrase());
        m.put("message", mensagem);
        if (status == 422 && "Falha na validacao dos campos".equals(mensagem)) {
            m.put("fieldErrors", List.of(Map.of("field", "nome", "message", "deve ter entre 2 e 60 caracteres")));
        }
        return m;
    }

    // ------------------------------------------------------------------
    // ABAS: descricao com exemplos e ordem fixa (Conta e login primeiro)
    // ------------------------------------------------------------------
    private void descreverAbas(OpenAPI openApi) {
        List<String> ordem = new java.util.ArrayList<>(ABAS.keySet());
        List<io.swagger.v3.oas.models.tags.Tag> tags = new java.util.ArrayList<>();
        if (openApi.getTags() != null) {
            tags.addAll(openApi.getTags());
        }
        // garante que toda aba conhecida exista na lista (com a descricao nova)
        for (String nome : ordem) {
            if (tags.stream().noneMatch(t -> nome.equals(t.getName()))) {
                tags.add(new io.swagger.v3.oas.models.tags.Tag().name(nome));
            }
        }
        for (io.swagger.v3.oas.models.tags.Tag t : tags) {
            String d = ABAS.get(t.getName());
            if (d != null) {
                t.setDescription(d);
            }
        }
        // so mantem as abas que tem rota neste grupo (v1/v2)
        java.util.Set<String> usadas = new java.util.HashSet<>();
        if (openApi.getPaths() != null) {
            openApi.getPaths().values().forEach(item -> item.readOperations().forEach(o -> {
                if (o.getTags() != null) {
                    usadas.addAll(o.getTags());
                }
            }));
        }
        tags.removeIf(t -> !usadas.contains(t.getName()));
        tags.sort(java.util.Comparator.comparingInt(t -> {
            int i = ordem.indexOf(t.getName());
            return i < 0 ? 99 : i;
        }));
        openApi.setTags(tags);
    }

    private static SecurityScheme esquemaSessao() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name(SessaoService.HEADER)
                .description("Token de login (43 caracteres). Crie a conta em `POST /auth/cadastro`, entre em "
                        + "`POST /auth/login` e cole aqui o campo `token` da resposta. Vale por 60 minutos e so "
                        + "e usado nas rotas da conta (`/auth/me`, `/auth/minha-chave` e `/auth/logout`).");
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
