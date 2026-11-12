# Changelog

Todas as mudanças relevantes da Lavander Love API ficam registradas aqui.
O formato segue o [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/) e o projeto usa
[Versionamento Semântico](https://semver.org/lang/pt-BR/):

- **MAJOR** (`X.0.0`): quebra compatibilidade, e o cliente precisa se adaptar.
- **MINOR** (`0.X.0`): funcionalidade nova compatível com quem já usa a API.
- **PATCH** (`0.0.X`): correção de bug, sem mudar o contrato.

> A versão da **aplicação** (abaixo) é diferente da versão do **contrato HTTP** (`X-API-Version`).
> O contrato v1 continua funcionando sem alteração; a v2 é escolhida pelo cliente via header.

## [2.1.0] - 2026-11-03

### Adicionado
- **Conta e login** (`/auth`): cadastro, login com token de sessão (`X-Session-Token`, 60 min) e logout.
  Senha guardada só como hash PBKDF2 com sal.
- **Chave de API pessoal**: `POST /auth/minha-chave` gera a chave da própria pessoa (uma por conta, nível
  ESCRITA, plano BASICO); gerar de novo troca o valor e o anterior para de funcionar.
- Nova entidade `Usuario`, ligada 1 para 1 com `ApiKey`.
- Front-end: aba **Minha conta** (entrar, criar conta, gerar e usar a chave).
- Limite de caracteres nos campos de texto (Bean Validation `@Size` + contador no front) e logo no front.

### Melhorado
- O front tira sozinho uma chave salva que não vale mais (ex.: depois de reiniciar a API) e avisa.
- No Swagger, o grupo v2 já manda `X-API-Version: 2` nas requisições.

### Corrigido
- Link HATEOAS da listagem v2 de produtos.

## [2.0.0] - 2026-11-01

Parte 2 do projeto: a API passa a ser tratada como um produto publicado, com proteção contra uso
indevido e evolução sem quebrar quem já usa.

### Adicionado
- **Chave de API (`X-API-Key`)**: nova entidade `ApiKey` com nível de acesso (`LEITURA`, `ESCRITA`,
  `ADMIN`) e plano de uso (`GRATUITO`, `BASICO`, `PROFISSIONAL`). A chave é guardada como hash SHA-256
  e mostrada uma única vez.
- Endpoints `/api-keys`: gerar, listar, buscar, `/me`, pesquisar, atualizar (inclusive plano e
  ativa/desativa), rotacionar e revogar.
- **Rate limit com token bucket**: por chave (tamanho do balde conforme o plano), por IP para quem não
  manda chave e um limite global. Headers `X-RateLimit-Limit`, `X-RateLimit-Remaining`,
  `X-RateLimit-Reset`, `X-RateLimit-Policy`; **429** com `Retry-After`.
- **Penalidade**: insistir depois do 429 (5 negadas seguidas) bloqueia o cliente por 30 s.
- `GET /limites`: endpoint público com a política de rate limit e o consumo atual de quem chamou.
- **Idempotência** em todo POST com `X-Idempotency-Key`: mesma chave e mesmo corpo devolvem a resposta
  original (`X-Idempotency-Replayed: true`); mesma chave com outro corpo dá 422; em processamento dá 409;
  sem a chave dá 400.
- **CORS** com origens, métodos e headers específicos (Live Server `:5500` liberado).
- **Versionamento por header `X-API-Version`**: `GET /produtos` e `GET /produtos/{id}` com v1 e v2.
  - v2: formato enxuto (`precoFormatado`, `disponivel`, categoria resumida) e **filtros combináveis** na
    listagem (`nome`, `categoriaId`, `precoMin`, `precoMax`, `disponivel`).
- Swagger: grupos v1/v2, botão Authorize, cadeado e nível exigido em cada rota, headers obrigatórios,
  respostas 400/401/403/429 e uma seção só de rate limit.
- Front-end em HTML + JS puro (`frontend/`) com aba de testes de todos os recursos da Parte 2.
- Testes JUnit do algoritmo de rate limit (`TokenBucketTest`).

### Melhorado
- `@ControllerAdvice` agora também trata JSON mal formado (400), parâmetro com tipo errado (400),
  header/parâmetro obrigatório ausente (400), método não suportado (405) e content-type não suportado (415).
- Erros gerados nos filtros (401, 403, 429, 400 de versão/idempotência) usam o mesmo formato `ApiError`.
- Links HATEOAS corretos atrás de proxy (Render), com `server.forward-headers-strategy=framework`.

### Corrigido
- Coluna do hash da chave nomeada `chave_hash` para não colidir com palavra reservada do H2.

## [1.0.0] - 2026-10

Parte 1 do projeto.

### Adicionado
- Entidades `Cliente`, `Endereco`, `Categoria`, `Produto` e `Pedido` (H2 + Spring Data JPA).
- CRUD completo com paginação, ordenação e consultas personalizadas (`/search`).
- HATEOAS (HAL) em todas as respostas.
- Bean Validation e `GlobalExceptionHandler` (404, 409, 422, 500).
- Documentação Swagger/Springdoc com tema customizado.
