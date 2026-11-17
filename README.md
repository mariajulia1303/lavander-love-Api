<p align="center">
  <img src="docs/banner.png" alt="Lavander Love - Floricultura API" width="600">
</p>

# Lavander Love — Floricultura API

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen)
![OpenAPI](https://img.shields.io/badge/OpenAPI-3.0-blue)
![License](https://img.shields.io/badge/license-MIT-green)

API RESTful da **Lavander Love**, uma floricultura. (Projeto final do curso de APIs com Spring Boot).
Representa o dia a dia de uma loja de flores: clientes, endereços, produtos, categorias e pedidos.

## Sumário

- [Versionamento](#versionamento) · [CHANGELOG](CHANGELOG.md)
- [Descrição geral](#descrição-geral)
- [Stack](#stack)
- [Como rodar](#como-rodar)
- [Links úteis](#links-úteis-com-a-aplicação-rodando)
- [Entidades e relacionamentos](#entidades-e-relacionamentos)
- [Estrutura do projeto](#estrutura-do-projeto)
- [Swagger UI customizado](#swagger-ui-customizado)
- [Endpoints](#endpoints)
- [Parte 2: recursos avançados](#parte-2-recursos-avançados)
- [Front-end](#front-end)
- [Tratamento de erros](#tratamento-de-erros)
- [Termos de serviço](#termos-de-serviço)
- [Contato](#contato)
- [Licença](#licença)

## Versionamento

| Item                    | Valor                                                        |
|-------------------------|--------------------------------------------------------------|
| Nome                    | Lavander Love — Floricultura API                             |
| Versão da API           | `2.0.0` (contrato HTTP: `X-API-Version` 1 e 2)               |
| Especificação           | **OpenAPI 3.0** (Swagger), gerada pelo Springdoc 2.6.0       |
| Documento da especificação | `http://localhost:8080/v3/api-docs/v1` e `/v3/api-docs/v2` |
| Padrão de versão        | [Versionamento Semântico](https://semver.org/lang/pt-BR/) (`MAJOR.MINOR.PATCH`) |

- **MAJOR**: mudanças que quebram compatibilidade (ex.: remover ou renomear um campo).
- **MINOR**: novas funcionalidades compatíveis (ex.: novo endpoint).
- **PATCH**: correções de bugs sem mudar o contrato.

O que mudou em cada versão está no [CHANGELOG.md](CHANGELOG.md) (novidades, melhorias e correções).

## Descrição geral

A Lavander Love API permite gerenciar clientes, endereços, produtos, categorias e pedidos de uma
floricultura por meio de endpoints REST com paginação, links HATEOAS e documentação Swagger.

### O que a API faz

Em resumo, ela cobre o ciclo completo de uma venda de flores, do cadastro ao pedido entregue:

- **Clientes e endereços:** cadastra e mantém os dados de quem compra (nome, e-mail e endereço de
  entrega). Cada cliente tem um endereço e pode ter vários pedidos.
- **Categorias:** organiza o catálogo em grupos (ex.: rosas, buquês, plantas). Uma categoria que ainda
  possui produtos não pode ser removida.
- **Produtos:** controla os itens à venda, com nome, preço e categoria, evitando nomes duplicados.
- **Pedidos:** registra a compra de um cliente com um ou mais produtos e acompanha o andamento pelo
  status (`PENDENTE` → `PAGO` → `EM_PREPARACAO` → `ENVIADO` → `ENTREGUE`, ou `CANCELADO`).
- **Consultas:** cada recurso tem listagem paginada e ordenável, busca por id e uma busca com filtros
  (`/search`) para encontrar registros específicos.
- **Navegação (HATEOAS):** as respostas trazem links para os recursos relacionados, facilitando
  seguir o fluxo (ex.: de um pedido para o cliente e seus produtos).
- **Erros claros:** respostas padronizadas com código HTTP e mensagem em português explicando o que
  deu errado.
- **Documentação interativa:** o Swagger UI permite testar todos os endpoints direto no navegador.

### Regras gerais

- **Formato:** todas as requisições e respostas usam JSON (`Content-Type: application/json`).
- **Codificação:** **UTF-8** em todo o tráfego. Acentos e caracteres especiais (ex.: `Rosa Vermelha`,
  `Orquídea`, `Ação`) são aceitos normalmente.
- **Idioma:** as mensagens de erro e a documentação estão em **português do Brasil (pt-BR)**.
  Os dados (nomes de produtos, categorias, observações etc.) podem ser escritos em qualquer idioma,
  desde que em UTF-8.
- **Nomes de campos:** em `camelCase` (ex.: `dataPedido`).
- **Datas e horas:** formato ISO 8601 (ex.: `2026-10-03T22:05:00`).
- **Valores monetários:** números decimais com ponto (ex.: `49.90`), em reais (BRL).
- **Paginação:** parâmetros `page`, `size` e `sort` (formato `campo,direção`, ex.: `nome,asc`).
- **Campos únicos:** nome (categoria/produto) e e-mail (cliente) não podem se repetir.
- **Validação:** os dados de entrada são validados com Bean Validation; falhas retornam `422`.

### Definições

| Termo        | Significado                                                                 |
|--------------|-----------------------------------------------------------------------------|
| **Cliente**  | Pessoa que compra na floricultura. Possui um endereço e vários pedidos.     |
| **Endereço** | Local de entrega/cadastro de um cliente (relação um-para-um).               |
| **Categoria**| Agrupamento de produtos (ex.: Rosas, Buquês, Plantas).                      |
| **Produto**  | Item vendido na loja; pertence a uma categoria.                             |
| **Pedido**   | Compra feita por um cliente, com um ou mais produtos e um status.           |
| **Status do pedido** | `PENDENTE`, `PAGO`, `EM_PREPARACAO`, `ENVIADO`, `ENTREGUE`, `CANCELADO`. |

## Stack

- Java 17
- Spring Boot 3.3.5
- Maven (com wrapper `mvnw` / `mvnw.cmd` — não precisa ter o Maven instalado)
- H2 em memória (console habilitado)
- Spring Data JPA
- Spring HATEOAS
- Springdoc OpenAPI 2.6.0 (Swagger)

## Como rodar

Pré-requisito: ter um **JDK 17** instalado.

### Pela linha de comando

```bash
# Windows
.\mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```

### Pelo IntelliJ IDEA

1. `File > Open` e selecione a pasta raiz do projeto (onde está o `pom.xml`).
2. Aguarde o IntelliJ importar como projeto Maven e baixar as dependências.
3. Em `File > Project Structure > Project`, confirme que o SDK aponta para um JDK 17.
4. Abra `FloriculturaApplication.java` e clique no ▶ ao lado do método `main`.

A aplicação sobe na porta **8080**.

## Links úteis (com a aplicação rodando)

- Swagger UI (tema roxo + logo): http://localhost:8080/swagger-theme/index.html
- Swagger UI (padrão): http://localhost:8080/swagger-ui.html
- Especificação OpenAPI 3 (JSON): http://localhost:8080/v3/api-docs/v1 e http://localhost:8080/v3/api-docs/v2
- Front-end: abra `frontend/index.html` com o Live Server (http://127.0.0.1:5500) — veja [Front-end](#front-end)
- Política de rate limit: http://localhost:8080/limites
- H2 console: http://localhost:8080/h2-console

Para conectar no H2 console use:

| Campo    | Valor                        |
|----------|------------------------------|
| JDBC URL | `jdbc:h2:mem:floricultura`   |
| User     | `sa`                         |
| Password | *(em branco)*                |

> O banco é em memória: os dados somem quando a aplicação é desligada. Na subida, o Hibernate
> cria as tabelas e o `data.sql` popula alguns dados iniciais (3 categorias, 3 produtos, 1
> endereço e 1 cliente) para a API não começar vazia.

## Entidades e relacionamentos

Pacote base: `com.floricultura.api`

| Entidade    | Relacionamentos |
|-------------|-----------------|
| **Cliente** | `@OneToOne` (cascade ALL) com Endereco; `@OneToMany` com Pedido |
| **Endereco**| lado inverso do One-to-One com Cliente |
| **Categoria**| lado "um" do One-to-Many com Produto |
| **Produto** | `@ManyToOne` para Categoria |
| **Pedido**  | `@ManyToOne` para Cliente + `@ManyToMany` com Produto; usa o enum `StatusPedido` |
| **ApiKey**  | entidade da Parte 2 (chaves de API); usa os enums `NivelAcesso` e `Plano` |

Os três tipos de relacionamento estão cobertos:

- **Um-para-um:** Cliente ↔ Endereco
- **Um-para-muitos:** Categoria → Produto e Cliente → Pedido
- **Muitos-para-muitos:** Pedido ↔ Produto

Enum `StatusPedido`: `PENDENTE`, `PAGO`, `EM_PREPARACAO`, `ENVIADO`, `ENTREGUE`, `CANCELADO`.

## Estrutura do projeto

```
src/main/java/com/floricultura/api
├── assembler    # montadores de links HATEOAS (um por entidade)
├── config       # OpenApiConfig (Swagger, grupos v1/v2) e CorsConfig
├── controller   # endpoints REST (6+ por entidade) + ApiKeyController e LimitesController
├── dto          # objetos de entrada (Request) com Bean Validation e respostas (ProdutoV2, chave criada)
├── entity       # entidades JPA + enums StatusPedido, NivelAcesso e Plano (ApiKey é a 6ª entidade)
├── exception    # ApiError, exceções de domínio e GlobalExceptionHandler
├── idempotency  # filtro e armazenamento do X-Idempotency-Key
├── ratelimit    # token bucket, baldes por cliente/global e filtro do 429
├── repository   # interfaces JpaRepository com as consultas personalizadas
├── security     # autenticação por X-API-Key, níveis de acesso e chaves iniciais
├── service      # regras de negócio
├── versioning   # filtro do X-API-Version e anotação @VersaoApi
└── web          # utilitários dos filtros (rotas da API, ordem, escrita de erro)

frontend/        # front-end em HTML + CSS + JS puro (index.html, styles.css, app.js)
src/test/java    # testes JUnit do algoritmo de rate limit (TokenBucketTest)
CHANGELOG.md     # histórico de versões (novidades, melhorias e correções)

src/main/resources
├── application.properties
├── data.sql                     # dados iniciais (seed)
└── static/swagger-theme         # Swagger UI customizado (tema roxo + logo)
    ├── index.html
    ├── theme-roxo.css
    ├── logo.svg
    └── logo.js
```

## Swagger UI customizado

Além da tela padrão, há uma versão com tema roxo e logo em
`http://localhost:8080/swagger-theme/index.html`. Os arquivos ficam em
`src/main/resources/static/swagger-theme`:

- **theme-roxo.css** — paleta roxa (as cores ficam em variáveis CSS no topo do arquivo, é só
  ajustar ali para mudar o tom).
- **logo.svg** — logo da floricultura (troque por sua própria imagem se quiser).
- **logo.js** — injeta o logo no topo da tela.
- **index.html** — página do Swagger UI que carrega os bundles padrão + o tema e o logo.

## Endpoints

Base paths: `/clientes`, `/enderecos`, `/produtos`, `/categorias`, `/pedidos` (mais `/api-keys` e `/limites`,
da Parte 2, descritos abaixo).
Cada recurso expõe 6 operações:

| Método / rota            | Ação                                    | Respostas                     |
|--------------------------|-----------------------------------------|-------------------------------|
| `POST /{recurso}`        | cria (header `Location`)                | 201 / 409 / 422               |
| `GET /{recurso}/{id}`    | busca por id                            | 200 / 404                     |
| `GET /{recurso}`         | listagem paginada                       | 200                           |
| `GET /{recurso}/search`  | consulta personalizada (exige filtro)   | 200 / 422                     |
| `PUT /{recurso}/{id}`    | atualiza                                | 200 / 404 / 409 / 422         |
| `DELETE /{recurso}/{id}` | remove                                  | 204 / 404 / 409               |

> **Dica (listagens paginadas):** o parâmetro `sort` usa o formato `campo,direção`
> (ex.: `nome,asc`). Informar um campo que não existe na entidade retorna **422**
> (com mensagem explicando o campo inválido) — use um campo real ou deixe o `sort` em branco.

## Tratamento de erros

Centralizado em `GlobalExceptionHandler` (`@RestControllerAdvice`). O corpo de erro segue o
formato da classe `ApiError` (`fieldErrors` só aparece nos erros de validação 422).

| Código | Quando acontece |
|--------|-----------------|
| **400 Bad Request** | JSON mal formado, parâmetro com tipo errado, `X-API-Version` não suportada ou POST sem `X-Idempotency-Key` |
| **401 Unauthorized** | rota protegida sem `X-API-Key`, ou chave inválida/desativada |
| **403 Forbidden** | a chave é válida mas o nível não basta (ex.: LEITURA tentando DELETE) |
| **404 Not Found** | recurso buscado por id não existe (GET/PUT/DELETE), ou rota/recurso estático inexistente |
| **409 Conflict**  | nome/email único duplicado, categoria com produtos que não pode ser removida, ou POST referenciando id relacionado inexistente |
| **422 Unprocessable Entity** | falha de validação (body ou parâmetros); quando o `/search` vem sem nenhum filtro; quando o `sort` referencia um campo inexistente; ou `X-Idempotency-Key` reutilizada com outro corpo |
| **429 Too Many Requests** | passou do limite de requisições (vem com `Retry-After`) |
| **500 Internal Server Error** | erro inesperado |

Decisões de projeto:

- Validação retorna **422** (e não o 400 padrão): o JSON chegou certo, os valores é que não.
- **POST nunca retorna 404:** um POST cria, não busca. Id relacionado inexistente vira 409.
- **`/search` exige filtro:** sem nenhum parâmetro ele devolve 422 avisando qual filtro informar.
- **`sort` inválido vira 422** (e não 500): ordenar por um campo que não existe devolve uma mensagem clara em vez de erro interno.

Exemplo de corpo de erro (422):

```json
{
  "timestamp": "...",
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Falha na validacao dos campos",
  "fieldErrors": [
    { "field": "email", "message": "must be a well-formed email address" }
  ]
}
```


## Parte 2: recursos avançados

Todos os recursos abaixo são implementados como **filtros de servlet** que rodam antes dos controllers,
nesta ordem: **CORS → versão → rate limit → chave de API → idempotência**. Eles só atuam nas rotas da
API (`/clientes`, `/enderecos`, `/produtos`, `/categorias`, `/pedidos`, `/api-keys`, `/limites`); Swagger, H2 console e
arquivos estáticos ficam de fora (`/limites` também passa por eles). Os erros dos filtros usam o mesmo formato `ApiError` do `@ControllerAdvice`.

### Autenticação com chave de API (`X-API-Key`)

- Nova entidade **`ApiKey`** com o enum **`NivelAcesso`** (`LEITURA` < `ESCRITA` < `ADMIN`) e o enum **`Plano`**
  (`GRATUITO`, `BASICO`, `PROFISSIONAL`, que define o rate limit).
- A chave **nunca é salva em texto puro**: o banco guarda só o hash SHA-256. O valor aparece uma única vez,
  na resposta de criação (igual GitHub/Stripe).
- Regras de acesso (classe `PoliticaAcesso`, usada pelo filtro **e** pelo Swagger):

| Rota | Nível exigido |
|------|---------------|
| `GET /produtos/**`, `GET /categorias/**` | público (catálogo) |
| `GET /limites` | público (política de rate limit) |
| `GET` dos demais recursos | `LEITURA` (clientes e pedidos têm dados pessoais — LGPD) |
| `POST` / `PUT` | `ESCRITA` |
| `DELETE` | `ADMIN` |
| `/api-keys/**` | `ADMIN` (exceto `GET /api-keys/me`, qualquer chave válida) |

- Sem chave em rota protegida ou chave inválida → **401**; chave sem nível suficiente → **403**.
- **Chaves iniciais** (só para desenvolvimento, definidas no `application.properties` e trocáveis por variável de ambiente):

| Chave | Nível | Plano | Variável de ambiente |
|-------|-------|-------|----------------------|
| `ll_admin_dev_123` | ADMIN | GRATUITO | `LAVANDER_ADMIN_KEY` |
| `ll_escrita_dev_123` | ESCRITA | GRATUITO | `LAVANDER_ESCRITA_KEY` |
| `ll_leitura_dev_123` | LEITURA | GRATUITO | `LAVANDER_LEITURA_KEY` |

- Cada chave tem um **nível** (o que ela pode fazer) e um **plano** (`GRATUITO`, `BASICO`, `PROFISSIONAL`:
  quantas requisições ela pode fazer, ver [Rate limiting](#rate-limiting)). Como a chave fica ligada ao cliente,
  quem abusa pode ter a chave desativada (`ativa: false`) sem adiantar trocar de IP.

Endpoints de gerenciamento (`/api-keys`, exigem ADMIN). Eles aparecem no Swagger, na seção
"Chaves de API", mas só funcionam com chave ADMIN (os outros recebem 401/403). Para escondê-los da
documentação, use `lavander.docs.mostrar-gerenciamento-chaves=false`.

| Método / rota | Ação |
|---------------|------|
| `POST /api-keys` | gera chave nova (`{"nome": "...", "nivel": "ESCRITA", "plano": "BASICO"}`) — 201 com o campo `chave` |
| `GET /api-keys` | lista paginada (mostra só o prefixo) |
| `GET /api-keys/{id}` | busca por id |
| `GET /api-keys/me` | dados da chave usada na requisição |
| `GET /api-keys/search?nivel=&ativa=` | consulta personalizada |
| `PUT /api-keys/{id}` | altera nome, nível, plano e `ativa` (desativar sem apagar) |
| `POST /api-keys/{id}/rotacionar` | gera um novo valor (o antigo para de funcionar) |
| `DELETE /api-keys/{id}` | revoga (não deixa revogar a própria chave nem a última ADMIN) |

### Conta e chave pessoal (`/auth`)

Quem não tem chave pode criar a própria, sem depender de um ADMIN:

| Método / rota | Ação |
|---------------|------|
| `POST /auth/cadastro` | cria a conta (`nome`, `email`, `senha` de 6 a 72 caracteres) — 201 / 409 e-mail já usado / 422 |
| `POST /auth/login` | devolve um `token` de sessão (60 min) — 200 / 401 e-mail ou senha errados |
| `GET /auth/me` | dados da conta e resumo da chave (header `X-Session-Token`) |
| `POST /auth/minha-chave` | gera a chave pessoal (uma por conta, nível ESCRITA, plano BASICO); se já existir, troca o valor |
| `POST /auth/logout` | encerra a sessão |

- A senha fica só como hash **PBKDF2 com sal** (classe `SenhaUtil`, testada em `SenhaUtilTest`).
- Login errado sempre responde a mesma mensagem, para não revelar quais e-mails têm conta.
- As rotas `/auth` não pedem `X-API-Key` nem `X-Idempotency-Key`, mas contam no rate limit por IP, o que
  também freia quem tenta adivinhar senha.
- Uma chave desativada por um ADMIN não pode ser renovada pela própria pessoa (409).
- Nível e plano da chave pessoal: `lavander.auth.nivel-chave-pessoal` e `lavander.auth.plano-chave-pessoal`.

### Idempotência (`X-Idempotency-Key`)

Todo **POST** exige o header `X-Idempotency-Key` (ex.: um UUID gerado pelo cliente).

| Situação | Resposta |
|----------|----------|
| POST sem o header | **400** |
| primeira vez com a chave | processa normalmente e guarda status + corpo da resposta |
| mesma chave + mesmo corpo | **não processa de novo**: devolve a resposta guardada + `X-Idempotency-Replayed: true` |
| mesma chave + corpo diferente | **422** (a chave já pertence a outra operação) |
| mesma chave enquanto a 1ª ainda processa | **409** |

- Só respostas 2xx ficam guardadas: se deu erro, a chave é liberada para nova tentativa.
- As chaves são separadas por cliente (chave de API ou IP) e por rota, e expiram em 24h
  (`lavander.idempotency.ttl-horas`). O armazenamento é em memória; com várias instâncias iria para Redis/banco.

```bash
curl -i -X POST http://localhost:8080/categorias \
  -H "X-API-Key: ll_escrita_dev_123" \
  -H "X-Idempotency-Key: 6b1f0c3e-1111-4c1a-9d2f-000000000001" \
  -H "Content-Type: application/json" \
  -d '{"nome":"Orquideas","descricao":"Orquideas em vaso"}'
# rode de novo o MESMO comando: volta a mesma categoria com X-Idempotency-Replayed: true
```

### Rate limiting

**Algoritmo: token bucket** (balde de fichas, o mesmo do Bucket4j), implementado na classe `TokenBucket`
(Java puro, com testes JUnit em `TokenBucketTest`).

- Cada cliente tem um balde. Cada requisição gasta **1 ficha** e o balde é **reabastecido aos poucos**.
- Dá pra fazer uma **rajada** até o tamanho do balde (bom para picos normais de uso); esvaziou, só no ritmo da recarga.
- Diferente da janela fixa, não existe o "estouro na virada da janela": a recarga é contínua.
- O filtro roda **antes** do controller e do banco: requisição negada não chega a ser processada, então a API
  continua de pé e respondendo 429 mesmo sob ataque.

Dois níveis:

- **Local (por cliente):** com chave de API válida o balde é da **chave**, do tamanho do **plano** dela; sem
  chave o balde é do **IP** (limite anônimo). Atrás de proxy (Render) o IP vem do `X-Forwarded-For`.
- **Global:** um balde único para a API inteira, pra ela não cair se muitos clientes vierem juntos (ex.: DDoS).

| Quem | Capacidade (de uma vez) | Recarga |
|------|-------------------------|---------|
| Sem chave (por IP) | 5 | 15 por minuto (1 a cada 4 s) |
| Plano `GRATUITO` | 8 | 20 por minuto (1 a cada 3 s) |
| Plano `BASICO` | 20 | 60 por minuto (1 por segundo) |
| Plano `PROFISSIONAL` | 50 | 240 por minuto (4 por segundo) |
| Global (API inteira) | 200 | 600 por minuto |

- Os limites são baixos de propósito: clicar ~10 vezes seguidas no Swagger já devolve 429 (as chaves de
  demonstração são do plano GRATUITO). Planos ficam em `Plano.java`; anônimo, global e penalidade em
  `lavander.rate-limit.*`.
- **Penalidade:** quem continua enviando depois do 429 (**5 negadas seguidas**) fica **bloqueado por 30 s**,
  mesmo que o balde recarregue. Abuso repetido pode levar à desativação da chave.
- Headers em toda resposta:

| Header | Significado |
|--------|-------------|
| `X-RateLimit-Limit` | capacidade do balde |
| `X-RateLimit-Remaining` | fichas que ainda restam |
| `X-RateLimit-Reset` | segundos até o balde encher de novo |
| `X-RateLimit-Policy` | plano e recarga, ex.: `GRATUITO;capacidade=8;recarga=20/min` |
| `Retry-After` | só no **429**: segundos até poder tentar de novo |

- **`GET /limites`** (público) devolve essa política e quanto o cliente que chamou ainda tem.
- Os baldes ficam em memória (zeram ao reiniciar). Com várias instâncias iriam para o Redis (ex.: Bucket4j + Redis).

```bash
for i in $(seq 1 12); do curl -s -o /dev/null -w "%{http_code} " http://localhost:8080/categorias; done
# 200 200 200 200 200 429 429 429 429 429 429 429   (sem chave: balde de 5)
```

### CORS

- Configurado em `CorsConfig` com um `CorsFilter` que roda **antes** dos outros filtros (assim o preflight
  `OPTIONS` não toma 401 e até os erros 401/403/429 chegam legíveis no front).
- Origens permitidas: `lavander.cors.allowed-origins` (padrão `http://localhost:5500`, `http://127.0.0.1:5500`
  — Live Server — e `http://localhost:8080`). Em produção use a variável `LAVANDER_CORS_ORIGINS`.
- Métodos: `GET, POST, PUT, DELETE, OPTIONS`. Headers aceitos: `Content-Type, Accept, X-API-Key, X-API-Version,
  X-Idempotency-Key`. Headers expostos ao JS: `Location`, `X-API-Version`, `X-RateLimit-*` (inclusive `X-RateLimit-Policy`), `Retry-After`,
  `X-Idempotency-Key`, `X-Idempotency-Replayed`.
- Origem não listada → o navegador bloqueia (preflight recusado com 403 "Invalid CORS request").

### Versionamento (`X-API-Version`)

- Sem header = **versão 1** (padrão, não quebra quem já usa a API). Valores aceitos: `1` e `2`;
  qualquer outro → **400**. A resposta sempre devolve `X-API-Version` com a versão que respondeu.
- **Endpoints com duas versões:** `GET /produtos` e `GET /produtos/{id}`.

| | v1 | v2 (`X-API-Version: 2`) |
|---|---|---|
| categoria | objeto completo | resumida (`id`, `nome`) |
| `precoFormatado` | — | `"R$ 89,90"` |
| `disponivel` | — | `true` se tem estoque |
| lista em | `_embedded.produtoList` | `_embedded.produtos` |
| filtros na listagem | — (só pelo `/search`, um filtro por vez) | `nome`, `categoriaId`, `precoMin`, `precoMax`, `disponivel`, **combináveis** |

- O roteamento usa `@GetMapping(headers = "X-API-Version=2")`. No Swagger há um seletor no topo com os
  grupos **v1** e **v2** (`/v3/api-docs/v1` e `/v3/api-docs/v2`).

```bash
curl http://localhost:8080/produtos/1                         # v1
curl http://localhost:8080/produtos/1 -H "X-API-Version: 2"   # v2
curl "http://localhost:8080/produtos?categoriaId=1&precoMax=100&disponivel=true&sort=preco,asc" -H "X-API-Version: 2"
```

- A v1 continua funcionando igual (compatibilidade com quem já usa a API). A v2 é uma evolução **MINOR**
  do contrato (`2.0.0` da aplicação), registrada no [CHANGELOG.md](CHANGELOG.md).

### Swagger

- Botão **Authorize** para informar a `X-API-Key`; cada endpoint mostra o cadeado e o nível exigido.
- Todos os POST mostram o header obrigatório `X-Idempotency-Key`; todos mostram `X-API-Version` e as respostas
  400/401/403/429 quando se aplicam (o 429 documenta `Retry-After` e `X-RateLimit-*`).
- A descrição geral tem uma seção só de **rate limit**, com a tabela de planos, os headers e a penalidade,
  e a tag **Rate limit** traz o `GET /limites`.

## Front-end

Front simples em **HTML + CSS + JavaScript puro**, na pasta `frontend/` (sem build, sem framework).

**Como rodar**

1. Suba a API (porta 8080).
2. No VS Code, instale a extensão **Live Server**, clique com o botão direito em `frontend/index.html` →
   **Open with Live Server**. Ela abre em `http://127.0.0.1:5500`, origem já liberada no CORS.
   (Alternativa: `cd frontend && python -m http.server 5500`.)
3. No topo, clique em uma das chaves de demonstração (ADMIN, ESCRITA, LEITURA) ou cole a sua.
   As chaves de demonstração são do plano GRATUITO (8 requisições de uma vez): se for navegar bastante pelo
   painel, gere na aba **Chaves de API** uma chave PROFISSIONAL e clique em **Usar esta chave**.

**O que tem**

- Abas de **Produtos, Categorias, Clientes, Pedidos** (listar paginado, buscar, criar, editar, excluir) e
  **Chaves de API** (gerar com nível e plano, trocar o plano, ativar/desativar, rotacionar, revogar).
- Com a versão **2** selecionada, a busca de produtos ganha os filtros da v2 (preço mín./máx. e disponibilidade).
- A paginação segue os links HATEOAS (`_links.next` / `_links.prev`) e editar/excluir usam `_links.update` /
  `_links.delete` devolvidos pela API.
- Todo POST manda um `X-Idempotency-Key` novo; em Pedidos há o botão **"Reenviar com a mesma chave"** que
  mostra a API devolvendo o mesmo pedido em vez de criar outro.
- Seletor de **versão** (troca a tabela de produtos entre v1 e v2) e barra com o **rate limit** (plano e fichas restantes).
- Aba **Recursos avançados**: dispara requisições até tomar 429 (e a penalidade), consulta `GET /limites`, testa idempotência (igual / diferente / sem
  header), compara v1 × v2, testa 401/403 com chaves diferentes e explica o CORS.
- Aba **Log** com cada requisição: headers enviados, headers recebidos e corpo da resposta.

## Termos de serviço

Ao usar a Lavander Love API em sua aplicação, você concorda com as regras abaixo.

### O que você deve fazer

- **Usar a API para o fim a que ela se destina:** gerenciar clientes, produtos, categorias e
  pedidos de uma floricultura (ou estudar e testar o projeto).
- **Enviar dados verdadeiros e válidos**, em JSON e UTF-8, respeitando as regras de validação.
  *Por quê:* garante que os cadastros e pedidos sejam consistentes e que a API responda corretamente.
- **Respeitar a privacidade dos dados** de clientes e endereços (nome, e-mail, endereço), usando-os
  apenas para o propósito do pedido e conforme a LGPD (Lei nº 13.709/2018).
  *Por quê:* são dados pessoais e o uso indevido causa prejuízo a terceiros.
- **Tratar os códigos de resposta** (404, 409, 422 etc.) na sua aplicação e evitar repetir
  requisições com erro.
  *Por quê:* reduz carga desnecessária e evita comportamentos inesperados.
- **Informar problemas encontrados** (falhas, vulnerabilidades) pelo e-mail de contato.
  *Por quê:* ajuda a melhorar a API para todos.

### O que não é permitido

Se você fizer qualquer uma das ações abaixo, o seu acesso poderá ser **bloqueado**, com ou sem aviso prévio:

- Enviar um volume abusivo de requisições (flood, scraping agressivo ou ataque de negação de serviço).
- Tentar invadir, explorar falhas, burlar validações ou acessar dados que não são seus.
- Inserir conteúdo ilegal, ofensivo, enganoso ou que viole direitos de terceiros.
- Cadastrar dados pessoais de terceiros sem autorização.
- Usar a API para fraude, spam ou qualquer atividade ilícita.
- Fazer engenharia reversa para prejudicar o serviço ou se passar pela Lavander Love.

> **Importante:** algumas ações, como excluir (`DELETE`) registros ou alterar (`PUT`) pedidos já
> pagos/enviados, **não devem ser feitas sem critério**, pois não podem ser desfeitas. Use o
> status `CANCELADO` em vez de apagar pedidos e confirme sempre antes de remover um registro.

### Uso aceitável e limites

- **Limites de uso (rate limit):** cada chave tem um plano com um limite de requisições; sem chave o
  limite é por IP (veja [Rate limiting](#rate-limiting) ou `GET /limites`). Ao receber **429**, espere o tempo
  do `Retry-After` antes de tentar de novo: insistir gera bloqueio temporário de 30 s e o abuso repetido pode
  levar à **desativação da chave**.
- **Uso justo:** use a API de forma razoável. Evite loops de requisições e consultas desnecessárias;
  prefira paginação (`page` e `size`) em vez de buscar tudo de uma vez.
- **Ambientes:** a instância padrão (H2 em memória) serve para estudo, testes e demonstração. Para
  uso real em produção, troque o banco por um persistente e proteja a API (autenticação e HTTPS).
- **Credenciais e segurança:** as chaves de API (`X-API-Key`) são pessoais e
  intransferíveis. Não compartilhe nem publique em repositórios abertos.
- **Responsabilidade pelos dados:** quem usa a API é responsável pelos dados que envia e pelo uso que
  faz das respostas dentro da sua própria aplicação.
- **Atribuição:** ao reutilizar o código, mantenha o aviso de copyright e a licença MIT.

### Privacidade e dados pessoais

- A API armazena dados pessoais de clientes (nome, e-mail e endereço) apenas para o funcionamento
  dos pedidos.
- Não coletamos dados além dos enviados nas requisições, e eles não são compartilhados com terceiros.
- Como o banco é em memória, os dados são descartados quando a aplicação é desligada.
- Quem integrar a API a uma aplicação real deve informar seus usuários sobre o tratamento dos dados e
  atender pedidos de correção ou exclusão, conforme a LGPD.

### Bloqueio, ajustes e disponibilidade

- Contas ou aplicações que violarem estes termos podem ser **bloqueadas** e ter o acesso encerrado.
- Podemos **cobrar valores extras ou aplicar limites de uso** (por exemplo, limite de requisições)
  para aplicações que utilizem a API de forma intensiva ou em ambiente de produção real, conforme
  o período e o volume de uso.
- Estes termos podem ser atualizados a qualquer momento; o uso continuado da API significa
  concordância com a versão vigente.
- Recursos, endpoints e regras podem mudar entre versões. Mudanças que quebram compatibilidade
  seguem o versionamento semântico (nova versão MAJOR).
- A Lavander Love não se responsabiliza por perdas de dados, pedidos não concluídos ou danos
  decorrentes do uso da API, que é oferecida sem garantia de funcionamento ininterrupto.
- Dúvidas sobre estes termos podem ser enviadas pelo e-mail da seção [Contato](#contato).
- A API roda com banco em memória (H2): **os dados são apagados ao reiniciar** e não há garantia de
  disponibilidade ou de persistência. O serviço é fornecido "como está", sem garantias.

## Contato

Encontrou um problema, bug ou tem alguma dúvida?

- **E-mail:** contato@lavanderlove.com.br
- Ao reportar, informe: endpoint, método, corpo da requisição e a resposta recebida.

## Licença

Este projeto está sob a **Licença MIT**.

A MIT é uma das licenças de software livre mais permissivas. Em resumo, qualquer pessoa pode:

-  usar o software, inclusive em projetos comerciais;
-  copiar, baixar e modificar o código;
-  distribuir e até vender cópias ou versões modificadas;
-  incluir o código em projetos próprios, inclusive de código fechado.

Em troca, existe **uma única exigência**: manter o aviso de copyright e o texto da licença em todas
as cópias ou partes substanciais do software. O software é fornecido **sem garantia** e os autores
não se responsabilizam por danos decorrentes do uso.

```
MIT License

Copyright (c) 2026 Lavander Love

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
