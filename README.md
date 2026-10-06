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

- [Versionamento](#versionamento)
- [Descrição geral](#descrição-geral)
- [Stack](#stack)
- [Como rodar](#como-rodar)
- [Links úteis](#links-úteis-com-a-aplicação-rodando)
- [Entidades e relacionamentos](#entidades-e-relacionamentos)
- [Estrutura do projeto](#estrutura-do-projeto)
- [Swagger UI customizado](#swagger-ui-customizado)
- [Endpoints](#endpoints)
- [Tratamento de erros](#tratamento-de-erros)
- [Termos de serviço](#termos-de-serviço)
- [Contato](#contato)
- [Licença](#licença)

## Versionamento

| Item                    | Valor                                                        |
|-------------------------|--------------------------------------------------------------|
| Nome                    | Lavander Love — Floricultura API                             |
| Versão da API           | `1.0.0`                                                      |
| Especificação           | **OpenAPI 3.0** (Swagger), gerada pelo Springdoc 2.6.0       |
| Documento da especificação | `http://localhost:8080/v3/api-docs`                       |
| Padrão de versão        | [Versionamento Semântico](https://semver.org/lang/pt-BR/) (`MAJOR.MINOR.PATCH`) |

- **MAJOR**: mudanças que quebram compatibilidade (ex.: remover ou renomear um campo).
- **MINOR**: novas funcionalidades compatíveis (ex.: novo endpoint).
- **PATCH**: correções de bugs sem mudar o contrato.

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
- Especificação OpenAPI 3 (JSON): http://localhost:8080/v3/api-docs
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

Os três tipos de relacionamento estão cobertos:

- **Um-para-um:** Cliente ↔ Endereco
- **Um-para-muitos:** Categoria → Produto e Cliente → Pedido
- **Muitos-para-muitos:** Pedido ↔ Produto

Enum `StatusPedido`: `PENDENTE`, `PAGO`, `EM_PREPARACAO`, `ENVIADO`, `ENTREGUE`, `CANCELADO`.

## Estrutura do projeto

```
src/main/java/com/floricultura/api
├── assembler    # montadores de links HATEOAS (um por entidade)
├── config       # OpenApiConfig (cabeçalho do Swagger)
├── controller   # endpoints REST (6 por entidade)
├── dto          # objetos de entrada (Request) com Bean Validation
├── entity       # as 5 entidades JPA + o enum StatusPedido
├── exception    # ApiError, exceções de domínio e GlobalExceptionHandler
├── repository   # interfaces JpaRepository com as consultas personalizadas
└── service      # regras de negócio

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

Base paths: `/clientes`, `/enderecos`, `/produtos`, `/categorias`, `/pedidos`.
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
| **404 Not Found** | recurso buscado por id não existe (GET/PUT/DELETE), ou rota/recurso estático inexistente |
| **409 Conflict**  | nome/email único duplicado, categoria com produtos que não pode ser removida, ou POST referenciando id relacionado inexistente |
| **422 Unprocessable Entity** | falha de validação (body ou parâmetros); quando o `/search` vem sem nenhum filtro; ou quando o `sort` referencia um campo inexistente |
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

- **Uso justo:** use a API de forma razoável. Evite loops de requisições e consultas desnecessárias;
  prefira paginação (`page` e `size`) em vez de buscar tudo de uma vez.
- **Ambientes:** a instância padrão (H2 em memória) serve para estudo, testes e demonstração. Para
  uso real em produção, troque o banco por um persistente e proteja a API (autenticação e HTTPS).
- **Credenciais e segurança:** se um dia houver chaves de acesso ou tokens, eles são pessoais e
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

- ✅ usar o software, inclusive em projetos comerciais;
- ✅ copiar, baixar e modificar o código;
- ✅ distribuir e até vender cópias ou versões modificadas;
- ✅ incluir o código em projetos próprios, inclusive de código fechado.

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
