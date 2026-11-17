// =====================================================================
// Lavander Love - front-end simples em HTML + JS puro (sem framework)
// Consome a API mostrando: chave de API, idempotencia, rate limit,
// versionamento por header, CORS e navegacao HATEOAS (_links).
// =====================================================================

const STATUS_PEDIDO = ["PENDENTE", "PAGO", "EM_PREPARACAO", "ENVIADO", "ENTREGUE", "CANCELADO"];
const TAMANHO_PAGINA = 8;

// ---------- configuracao (fica salva no navegador) ----------
// quando o painel e aberto pela propria API (ex.: https://lavander-love-api.onrender.com/ ou
// http://localhost:8080/), a API e o mesmo endereco da pagina. Aberto pelo Live Server (:5500)
// ou pelo IntelliJ (:63342), usa o endereco salvo ou o localhost:8080.
function urlInicialDaApi() {
  const servidoPelaApi = location.protocol.startsWith("http") && !["5500", "63342", "63343"].includes(location.port);
  if (servidoPelaApi) return location.origin;
  return lerStorage("ll.base") || "http://localhost:8080";
}

const cfg = {
  base: urlInicialDaApi(),
  key: lerStorage("ll.key") || "",
  versao: lerStorage("ll.versao") || "1",
};

function lerStorage(k) {
  try { return localStorage.getItem(k); } catch { return null; }
}
function salvarStorage(k, v) {
  try { localStorage.setItem(k, v); } catch { /* navegador sem storage: segue so em memoria */ }
}

// ---------- utilitarios ----------
const $ = (sel, el = document) => el.querySelector(sel);
const $$ = (sel, el = document) => [...el.querySelectorAll(sel)];

function esc(v) {
  return String(v ?? "").replace(/[&<>"']/g, (c) =>
    ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
}

function uuid() {
  if (crypto.randomUUID) return crypto.randomUUID();
  return "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0;
    return (c === "x" ? r : (r & 0x3) | 0x8).toString(16);
  });
}

const moeda = (v) => Number(v ?? 0).toLocaleString("pt-BR", { style: "currency", currency: "BRL" });
const dataHora = (v) => (v ? new Date(v).toLocaleString("pt-BR") : "—");

function avisar(msg, tipo = "ok") {
  // antes do login o aviso aparece na propria tela de entrada
  const el = document.body.classList.contains("logado") ? $("#aviso") : $("#entrada-aviso");
  el.textContent = msg;
  el.className = "aviso " + tipo;
  clearTimeout(avisar.t);
  avisar.t = setTimeout(() => el.classList.add("escondido"), 6000);
}

// erro da API ja traduzido pra uma frase
function mensagemErro(e) {
  if (!(e instanceof ApiErro)) return e.message || String(e);
  const d = e.dados;
  let msg = `${e.status} - ${d?.message || d?.error || "Erro"}`;
  if (d?.fieldErrors?.length) msg += ": " + d.fieldErrors.map((f) => `${f.field} ${f.message}`).join("; ");
  return msg;
}

class ApiErro extends Error {
  constructor(status, dados, headers) {
    super(dados?.message || `HTTP ${status}`);
    this.status = status;
    this.dados = dados;
    this.headers = headers;
  }
}

// =====================================================================
// cliente HTTP: TODA chamada passa por aqui
// =====================================================================
const log = [];

async function api(metodo, caminho, opcoes = {}) {
  const { corpo, idemKey, versao, chave, sessao } = opcoes;
  const url = caminho.startsWith("http") ? caminho : cfg.base.replace(/\/$/, "") + caminho;

  const headers = { Accept: "application/hal+json, application/json" };
  const chaveUsada = chave !== undefined ? chave : cfg.key;
  if (chaveUsada) headers["X-API-Key"] = chaveUsada;
  headers["X-API-Version"] = versao || cfg.versao;
  if (sessao) headers["X-Session-Token"] = sessao;
  if (corpo !== undefined) headers["Content-Type"] = "application/json";
  // idempotencia: todo POST leva uma chave unica (ou a que foi passada, pra reenviar)
  // (idemKey: null = nao manda o header, usado so no teste de erro)
  if (metodo === "POST" && idemKey !== null) headers["X-Idempotency-Key"] = idemKey || uuid();

  const inicio = performance.now();
  let res;
  try {
    res = await fetch(url, { method: metodo, headers, body: corpo !== undefined ? JSON.stringify(corpo) : undefined });
  } catch (err) {
    // fetch so lanca erro quando nem chegou resposta legivel: API fora do ar OU bloqueio de CORS
    registrar({ metodo, url, status: 0, headers, dados: String(err), ms: performance.now() - inicio });
    atualizarUltima(0);
    throw new Error("Não foi possível falar com a API (está rodando? a origem está liberada no CORS?).");
  }

  const dados = res.status === 204 ? null : await res.json().catch(() => null);
  const respHeaders = {};
  ["X-API-Version", "X-RateLimit-Limit", "X-RateLimit-Remaining", "X-RateLimit-Reset", "X-RateLimit-Policy", "Retry-After",
    "X-Idempotency-Key", "X-Idempotency-Replayed", "Location"].forEach((h) => {
    const v = res.headers.get(h);
    if (v !== null) respHeaders[h] = v;
  });

  registrar({ metodo, url, status: res.status, headers, respHeaders, corpo, dados, ms: performance.now() - inicio });
  atualizarRate(respHeaders);
  atualizarUltima(res.status, respHeaders);

  if (!res.ok) throw new ApiErro(res.status, dados, respHeaders);
  return { dados, headers: respHeaders, status: res.status };
}

// itens de uma resposta paginada HAL (_embedded.<qualquerNome>)
function itens(pagina) {
  return pagina?._embedded ? Object.values(pagina._embedded)[0] || [] : [];
}

// ---------- barra de status ----------
function atualizarRate(h) {
  if (h["X-RateLimit-Limit"] === undefined) return;
  const el = $("#st-rate");
  const rest = Number(h["X-RateLimit-Remaining"]);
  const lim = Number(h["X-RateLimit-Limit"]);
  const plano = (h["X-RateLimit-Policy"] || "").split(";")[0];
  const reset = Number(h["X-RateLimit-Reset"]);
  el.textContent = `Rate limit${plano ? " (" + plano + ")" : ""}: ${rest}/${lim} fichas`
    + (reset > 0 ? ` · balde cheio em ${reset}s` : " · balde cheio");
  el.className = "chip " + (rest === 0 ? "ruim" : rest < lim * 0.25 ? "alerta" : "ok");
}

function atualizarUltima(status, h = {}) {
  const el = $("#st-ultima");
  let txt = `Última resposta: ${status || "falha de rede/CORS"}`;
  if (h["X-API-Version"]) txt += ` · v${h["X-API-Version"]}`;
  if (h["X-Idempotency-Replayed"]) txt += " · repetida (idempotência)";
  el.textContent = txt;
  el.className = "chip " + (status >= 200 && status < 300 ? "ok" : status === 429 || status >= 400 ? "ruim" : "");
}

// a aba "Chaves de API" lista as chaves de TODOS os usuarios: so aparece pra quem esta com chave ADMIN.
// (a propria API tambem barra: /api-keys exige ADMIN, senao 401/403)
function atualizarAbaChaves() {
  const admin = cfg.nivel === "ADMIN";
  $('.abas button[data-aba="chaves"]').classList.toggle("escondido", !admin);
  if (!admin && abaAtual === "chaves") abrirAba("conta");
}

async function verificarChave() {
  try { await verificarChaveInterno(); } finally { atualizarAbaChaves(); }
}

async function verificarChaveInterno() {
  const el = $("#st-chave");
  if (!cfg.key) {
    el.textContent = "Sem chave (só leitura pública)";
    el.className = "chip";
    cfg.nivel = null;
    return;
  }
  try {
    const { dados } = await api("GET", "/api-keys/me");
    el.textContent = `Chave: ${dados.nome} · ${dados.nivel} · plano ${dados.plano || "GRATUITO"}`;
    el.className = "chip ok";
    cfg.nivel = dados.nivel;
  } catch (e) {
    cfg.nivel = null;
    if (e instanceof ApiErro && e.status === 401) {
      // a chave salva no navegador nao existe mais (ex: a API foi reiniciada e o banco em memoria zerou).
      // tira ela daqui, senao TODA requisicao volta 401, ate as publicas
      cfg.key = "";
      $("#cfg-key").value = "";
      salvarStorage("ll.key", "");
      el.textContent = "Sem chave";
      el.className = "chip";
      avisar("A chave que estava salva não vale mais (a API foi reiniciada?). Entre em \"Minha conta\" e gere uma nova.", "erro");
    } else {
      el.textContent = "Não deu pra conferir a chave";
      el.className = "chip ruim";
    }
  }
}

// ---------- log ----------
function registrar(entrada) {
  log.unshift({ ...entrada, quando: new Date() });
  if (log.length > 200) log.pop();
  if (!$("#aba-log").classList.contains("escondido")) renderLog();
}

function renderLog() {
  $("#log-lista").innerHTML = log.length ? log.map((l) => {
    const classe = "c" + String(l.status)[0];
    const detalhes = {
      "headers enviados": l.headers,
      ...(l.corpo !== undefined ? { "corpo enviado": l.corpo } : {}),
      "headers recebidos": l.respHeaders,
      resposta: l.dados,
    };
    return `<details class="log-item">
      <summary><span class="metodo">${esc(l.metodo)}</span>
        <span class="cod ${classe}">${l.status || "ERRO"}</span>
        <span>${esc(l.url)}</span>
        <span class="dica">${l.quando.toLocaleTimeString("pt-BR")} · ${Math.round(l.ms)}ms</span></summary>
      <pre class="saida">${esc(JSON.stringify(detalhes, null, 2))}</pre>
    </details>`;
  }).join("") : `<p class="dica">Nenhuma requisição ainda.</p>`;
}

// ---------- paginacao usando os links HATEOAS ----------
function renderPaginacao(alvo, pagina, carregar) {
  const el = $(alvo);
  const p = pagina?.page;
  if (!p) { el.innerHTML = ""; return; }
  const links = pagina._links || {};
  el.innerHTML = `<span>Página ${p.totalPages ? p.number + 1 : 0} de ${p.totalPages} · ${p.totalElements} registro(s)</span>
    <button class="secundario mini" data-href="${esc(links.prev?.href || "")}" ${links.prev ? "" : "disabled"}>‹ Anterior</button>
    <button class="secundario mini" data-href="${esc(links.next?.href || "")}" ${links.next ? "" : "disabled"}>Próxima ›</button>`;
  // segue o link "next"/"prev" que a propria API mandou (navegabilidade HATEOAS)
  $$("button[data-href]", el).forEach((b) => b.addEventListener("click", () => b.dataset.href && carregar(b.dataset.href)));
}

function linhaVazia(tbody, colunas, texto) {
  $(tbody).innerHTML = `<tr><td class="vazio" colspan="${colunas}">${esc(texto)}</td></tr>`;
}

function dadosDoForm(form) {
  return Object.fromEntries(new FormData(form).entries());
}

function resetarForm(form, titulo) {
  form.reset();
  if (form.elements.id) form.elements.id.value = "";
  $("h3", form).textContent = titulo;
}

async function excluir(href, rotulo, recarregar) {
  if (!confirm(`Excluir ${rotulo}? Essa ação não pode ser desfeita.`)) return;
  try {
    await api("DELETE", href);
    avisar(`${rotulo} excluído(a).`);
    recarregar();
  } catch (e) { avisar(mensagemErro(e), "erro"); }
}

// =====================================================================
// PRODUTOS
// =====================================================================
const produtos = { url: null, cache: new Map() };

async function carregarProdutos(url) {
  produtos.url = url || `/produtos?page=0&size=${TAMANHO_PAGINA}&sort=nome,asc`;
  const v2 = cfg.versao === "2";
  $("#prod-versao").textContent = `· exibindo formato da versão ${cfg.versao}`;
  $$("#prod-busca [data-v2]").forEach((el) => el.classList.toggle("escondido", !v2));
  $("#prod-thead").innerHTML = v2
    ? "<tr><th>#</th><th>Nome</th><th>Preço formatado</th><th>Disponível</th><th>Categoria</th><th></th></tr>"
    : "<tr><th>#</th><th>Nome</th><th>Preço</th><th>Estoque</th><th>Categoria</th><th></th></tr>";
  try {
    const { dados } = await api("GET", produtos.url);
    const lista = itens(dados);
    produtos.cache.clear();
    if (!lista.length) linhaVazia("#prod-tbody", 6, "Nenhum produto encontrado.");
    else $("#prod-tbody").innerHTML = lista.map((p) => {
      produtos.cache.set(String(p.id), p);
      const meio = v2
        ? `<td>${esc(p.precoFormatado)}</td><td><span class="selo ${p.disponivel ? "ok" : "nao"}">${p.disponivel ? "sim" : "esgotado"}</span></td>`
        : `<td>${moeda(p.preco)}</td><td>${esc(p.estoque)}</td>`;
      return `<tr><td>${p.id}</td><td>${esc(p.nome)}</td>${meio}<td>${esc(p.categoria?.nome || "—")}</td>
        <td class="acoes-linha">
          <button class="secundario mini" data-editar="${p.id}">Editar</button>
          <button class="perigo mini" data-excluir="${esc(p._links?.delete?.href || "")}" data-nome="${esc(p.nome)}">Excluir</button>
        </td></tr>`;
    }).join("");
    renderPaginacao("#prod-pag", dados, carregarProdutos);
  } catch (e) {
    linhaVazia("#prod-tbody", 6, mensagemErro(e));
    $("#prod-pag").innerHTML = "";
  }
}

async function carregarOpcoesCategorias() {
  try {
    const { dados } = await api("GET", "/categorias?size=100&sort=nome,asc");
    $("#prod-form select[name=categoriaId]").innerHTML =
      itens(dados).map((c) => `<option value="${c.id}">${esc(c.nome)}</option>`).join("");
  } catch { /* sem categorias ainda */ }
}

function ligarProdutos() {
  const form = $("#prod-form");
  form.addEventListener("submit", async (ev) => {
    ev.preventDefault();
    const d = dadosDoForm(form);
    const corpo = { nome: d.nome, preco: Number(d.preco), estoque: Number(d.estoque), categoriaId: Number(d.categoriaId) };
    try {
      if (d.id) await api("PUT", `/produtos/${d.id}`, { corpo });
      else await api("POST", "/produtos", { corpo });
      avisar(d.id ? "Produto atualizado." : "Produto criado.");
      resetarForm(form, "Novo produto");
      carregarProdutos(produtos.url);
    } catch (e) { avisar(mensagemErro(e), "erro"); }
  });
  $("[data-cancelar]", form).addEventListener("click", () => resetarForm(form, "Novo produto"));

  $("#prod-tbody").addEventListener("click", (ev) => {
    const b = ev.target.closest("button");
    if (!b) return;
    if (b.dataset.editar) {
      const p = produtos.cache.get(b.dataset.editar);
      form.elements.id.value = p.id;
      form.elements.nome.value = p.nome;
      form.elements.preco.value = p.preco;
      form.elements.estoque.value = p.estoque;
      if (p.categoria) form.elements.categoriaId.value = p.categoria.id;
      $("h3", form).textContent = `Editando produto #${p.id}`;
    } else if (b.dataset.excluir) {
      excluir(b.dataset.excluir, `o produto "${b.dataset.nome}"`, () => carregarProdutos(produtos.url));
    }
  });

  $("#prod-busca").addEventListener("submit", (ev) => {
    ev.preventDefault();
    const f = ev.target.elements;
    const nome = f.nome.value.trim();
    if (cfg.versao === "2") {
      // v2: filtros combinaveis direto na listagem (GET /produtos?nome=&precoMax=&disponivel=)
      const q = new URLSearchParams({ page: 0, size: TAMANHO_PAGINA, sort: "nome,asc" });
      if (nome) q.set("nome", nome);
      if (f.precoMin.value) q.set("precoMin", f.precoMin.value);
      if (f.precoMax.value) q.set("precoMax", f.precoMax.value);
      if (f.disponivel.value) q.set("disponivel", f.disponivel.value);
      carregarProdutos(`/produtos?${q}`);
      return;
    }
    carregarProdutos(nome ? `/produtos/search?nome=${encodeURIComponent(nome)}&size=${TAMANHO_PAGINA}` : null);
  });
  $("#prod-busca [data-limpar]").addEventListener("click", () => {
    $("#prod-busca").reset();
    carregarProdutos();
  });
}

// =====================================================================
// CATEGORIAS
// =====================================================================
const categorias = { url: null, cache: new Map() };

async function carregarCategorias(url) {
  categorias.url = url || `/categorias?page=0&size=${TAMANHO_PAGINA}&sort=nome,asc`;
  try {
    const { dados } = await api("GET", categorias.url);
    const lista = itens(dados);
    categorias.cache.clear();
    if (!lista.length) linhaVazia("#cat-tbody", 4, "Nenhuma categoria.");
    else $("#cat-tbody").innerHTML = lista.map((c) => {
      categorias.cache.set(String(c.id), c);
      return `<tr><td>${c.id}</td><td>${esc(c.nome)}</td><td>${esc(c.descricao || "")}</td>
        <td class="acoes-linha">
          <button class="secundario mini" data-editar="${c.id}">Editar</button>
          <button class="perigo mini" data-excluir="${esc(c._links?.delete?.href || "")}" data-nome="${esc(c.nome)}">Excluir</button>
        </td></tr>`;
    }).join("");
    renderPaginacao("#cat-pag", dados, carregarCategorias);
  } catch (e) { linhaVazia("#cat-tbody", 4, mensagemErro(e)); }
}

function ligarCategorias() {
  const form = $("#cat-form");
  form.addEventListener("submit", async (ev) => {
    ev.preventDefault();
    const d = dadosDoForm(form);
    const corpo = { nome: d.nome, descricao: d.descricao };
    try {
      if (d.id) await api("PUT", `/categorias/${d.id}`, { corpo });
      else await api("POST", "/categorias", { corpo });
      avisar(d.id ? "Categoria atualizada." : "Categoria criada.");
      resetarForm(form, "Nova categoria");
      carregarCategorias(categorias.url);
      carregarOpcoesCategorias();
    } catch (e) { avisar(mensagemErro(e), "erro"); }
  });
  $("[data-cancelar]", form).addEventListener("click", () => resetarForm(form, "Nova categoria"));
  $("#cat-tbody").addEventListener("click", (ev) => {
    const b = ev.target.closest("button");
    if (!b) return;
    if (b.dataset.editar) {
      const c = categorias.cache.get(b.dataset.editar);
      form.elements.id.value = c.id;
      form.elements.nome.value = c.nome;
      form.elements.descricao.value = c.descricao || "";
      $("h3", form).textContent = `Editando categoria #${c.id}`;
    } else if (b.dataset.excluir) {
      excluir(b.dataset.excluir, `a categoria "${b.dataset.nome}"`, () => carregarCategorias(categorias.url));
    }
  });
}

// =====================================================================
// CLIENTES
// =====================================================================
const clientes = { url: null, cache: new Map() };
const CAMPOS_ENDERECO = ["logradouro", "numero", "bairro", "cidade", "uf", "cep"];

async function carregarClientes(url) {
  clientes.url = url || `/clientes?page=0&size=${TAMANHO_PAGINA}&sort=nome,asc`;
  try {
    const { dados } = await api("GET", clientes.url);
    const lista = itens(dados);
    clientes.cache.clear();
    if (!lista.length) linhaVazia("#cli-tbody", 6, "Nenhum cliente.");
    else $("#cli-tbody").innerHTML = lista.map((c) => {
      clientes.cache.set(String(c.id), c);
      return `<tr><td>${c.id}</td><td>${esc(c.nome)}</td><td>${esc(c.email)}</td><td>${esc(c.telefone)}</td>
        <td>${esc(c.endereco ? `${c.endereco.cidade}/${c.endereco.uf}` : "—")}</td>
        <td class="acoes-linha">
          <button class="secundario mini" data-editar="${c.id}">Editar</button>
          <button class="perigo mini" data-excluir="${esc(c._links?.delete?.href || "")}" data-nome="${esc(c.nome)}">Excluir</button>
        </td></tr>`;
    }).join("");
    renderPaginacao("#cli-pag", dados, carregarClientes);
  } catch (e) {
    linhaVazia("#cli-tbody", 6, mensagemErro(e));
    $("#cli-pag").innerHTML = "";
  }
}

function ligarClientes() {
  const form = $("#cli-form");
  form.addEventListener("submit", async (ev) => {
    ev.preventDefault();
    const d = dadosDoForm(form);
    const endereco = Object.fromEntries(CAMPOS_ENDERECO.map((k) => [k, d[k]]));
    endereco.uf = (endereco.uf || "").toUpperCase();
    const corpo = { nome: d.nome, email: d.email, telefone: d.telefone, endereco };
    try {
      if (d.id) await api("PUT", `/clientes/${d.id}`, { corpo });
      else await api("POST", "/clientes", { corpo });
      avisar(d.id ? "Cliente atualizado." : "Cliente criado.");
      resetarForm(form, "Novo cliente");
      carregarClientes(clientes.url);
    } catch (e) { avisar(mensagemErro(e), "erro"); }
  });
  $("[data-cancelar]", form).addEventListener("click", () => resetarForm(form, "Novo cliente"));
  $("#cli-tbody").addEventListener("click", (ev) => {
    const b = ev.target.closest("button");
    if (!b) return;
    if (b.dataset.editar) {
      const c = clientes.cache.get(b.dataset.editar);
      form.elements.id.value = c.id;
      ["nome", "email", "telefone"].forEach((k) => (form.elements[k].value = c[k] || ""));
      CAMPOS_ENDERECO.forEach((k) => (form.elements[k].value = c.endereco?.[k] || ""));
      $("h3", form).textContent = `Editando cliente #${c.id}`;
    } else if (b.dataset.excluir) {
      excluir(b.dataset.excluir, `o cliente "${b.dataset.nome}"`, () => carregarClientes(clientes.url));
    }
  });
}

// =====================================================================
// PEDIDOS
// =====================================================================
const pedidos = { url: null, cache: new Map(), ultimoPost: null };

async function carregarPedidos(url) {
  pedidos.url = url || `/pedidos?page=0&size=${TAMANHO_PAGINA}&sort=dataPedido,desc`;
  try {
    const { dados } = await api("GET", pedidos.url);
    const lista = itens(dados);
    pedidos.cache.clear();
    if (!lista.length) linhaVazia("#ped-tbody", 6, "Nenhum pedido.");
    else $("#ped-tbody").innerHTML = lista.map((p) => {
      pedidos.cache.set(String(p.id), p);
      const total = (p.produtos || []).reduce((s, x) => s + Number(x.preco || 0), 0);
      const opcoes = STATUS_PEDIDO.map((s) => `<option ${s === p.status ? "selected" : ""}>${s}</option>`).join("");
      return `<tr><td>${p.id}<br><span class="dica">${dataHora(p.dataPedido)}</span></td>
        <td>${esc(p.cliente?.nome || "—")}</td>
        <td>${(p.produtos || []).map((x) => esc(x.nome)).join("<br>")}</td>
        <td>${moeda(total)}</td>
        <td><select data-status="${p.id}">${opcoes}</select></td>
        <td class="acoes-linha">
          <button class="perigo mini" data-excluir="${esc(p._links?.delete?.href || "")}" data-nome="#${p.id}">Excluir</button>
        </td></tr>`;
    }).join("");
    renderPaginacao("#ped-pag", dados, carregarPedidos);
  } catch (e) {
    linhaVazia("#ped-tbody", 6, mensagemErro(e));
    $("#ped-pag").innerHTML = "";
  }
}

async function carregarOpcoesPedido() {
  const form = $("#ped-form");
  try {
    const { dados } = await api("GET", "/clientes?size=100&sort=nome,asc");
    form.elements.clienteId.innerHTML = itens(dados).map((c) => `<option value="${c.id}">${esc(c.nome)}</option>`).join("");
  } catch (e) {
    form.elements.clienteId.innerHTML = `<option value="">(${esc(mensagemErro(e))})</option>`;
  }
  try {
    const { dados } = await api("GET", "/produtos?size=100&sort=nome,asc", { versao: "1" });
    $("#ped-produtos").innerHTML = itens(dados).map((p) =>
      `<label><input type="checkbox" value="${p.id}"> ${esc(p.nome)} <span class="dica">${moeda(p.preco)}</span></label>`).join("");
  } catch { $("#ped-produtos").innerHTML = ""; }
}

function ligarPedidos() {
  const form = $("#ped-form");
  form.elements.status.innerHTML = STATUS_PEDIDO.map((s) => `<option>${s}</option>`).join("");
  $("#ped-filtro select").innerHTML += STATUS_PEDIDO.map((s) => `<option>${s}</option>`).join("");

  form.addEventListener("submit", async (ev) => {
    ev.preventDefault();
    const produtoIds = $$("#ped-produtos input:checked").map((i) => Number(i.value));
    const corpo = { clienteId: Number(form.elements.clienteId.value), produtoIds, status: form.elements.status.value };
    // guarda a chave de idempotencia pra poder reenviar a MESMA operacao
    const idemKey = uuid();
    try {
      const { dados } = await api("POST", "/pedidos", { corpo, idemKey });
      pedidos.ultimoPost = { corpo, idemKey };
      $("#ped-idem-chave").textContent = idemKey;
      $("#ped-idem").classList.remove("escondido");
      avisar(`Pedido #${dados.id} criado.`);
      carregarPedidos(pedidos.url);
    } catch (e) { avisar(mensagemErro(e), "erro"); }
  });

  $("#ped-reenviar").addEventListener("click", async () => {
    const u = pedidos.ultimoPost;
    if (!u) return;
    try {
      const { dados, headers } = await api("POST", "/pedidos", { corpo: u.corpo, idemKey: u.idemKey });
      avisar(headers["X-Idempotency-Replayed"]
        ? `Repetição detectada: a API devolveu o mesmo pedido #${dados.id}, sem criar outro.`
        : `Pedido #${dados.id} criado.`);
      carregarPedidos(pedidos.url);
    } catch (e) { avisar(mensagemErro(e), "erro"); }
  });

  $("#ped-tbody").addEventListener("change", async (ev) => {
    const sel = ev.target.closest("select[data-status]");
    if (!sel) return;
    const p = pedidos.cache.get(sel.dataset.status);
    const corpo = { clienteId: p.cliente.id, produtoIds: p.produtos.map((x) => x.id), status: sel.value };
    try {
      await api("PUT", p._links.update.href, { corpo });
      avisar(`Pedido #${p.id} agora está ${sel.value}.`);
    } catch (e) {
      sel.value = p.status;
      avisar(mensagemErro(e), "erro");
    }
  });
  $("#ped-tbody").addEventListener("click", (ev) => {
    const b = ev.target.closest("button[data-excluir]");
    if (b) excluir(b.dataset.excluir, `o pedido ${b.dataset.nome}`, () => carregarPedidos(pedidos.url));
  });
  $("#ped-filtro").addEventListener("submit", (ev) => {
    ev.preventDefault();
    const s = ev.target.elements.status.value;
    carregarPedidos(s ? `/pedidos/search?status=${s}&size=${TAMANHO_PAGINA}` : null);
  });
}

// =====================================================================
// CHAVES DE API
// =====================================================================
const chaves = { url: null, cache: new Map() };
const PLANOS = ["GRATUITO", "BASICO", "PROFISSIONAL"];

async function carregarChaves(url) {
  chaves.url = url || `/api-keys?page=0&size=${TAMANHO_PAGINA}&sort=id,asc`;
  try {
    const { dados } = await api("GET", chaves.url);
    const lista = itens(dados);
    chaves.cache.clear();
    if (!lista.length) linhaVazia("#key-tbody", 8, "Nenhuma chave.");
    else $("#key-tbody").innerHTML = lista.map((k) => {
      chaves.cache.set(String(k.id), k);
      return `<tr><td>${k.id}</td><td>${esc(k.nome)}</td><td><code>${esc(k.prefixo)}</code></td>
        <td><span class="selo">${esc(k.nivel)}</span></td>
        <td><select class="mini-select" data-plano="${k.id}" title="Trocar o plano muda o rate limit">
          ${PLANOS.map((p) => `<option ${p === (k.plano || "GRATUITO") ? "selected" : ""}>${p}</option>`).join("")}
        </select></td>
        <td><span class="selo ${k.ativa ? "ok" : "nao"}">${k.ativa ? "sim" : "não"}</span></td>
        <td>${dataHora(k.ultimoUso)}</td>
        <td class="acoes-linha">
          <button class="secundario mini" data-alternar="${k.id}">${k.ativa ? "Desativar" : "Ativar"}</button>
          <button class="secundario mini" data-rotacionar="${k.id}">Rotacionar</button>
          <button class="perigo mini" data-excluir="${esc(k._links?.delete?.href || "")}" data-nome="${esc(k.nome)}" title="Apaga a chave (DELETE /api-keys/{id})">Excluir</button>
        </td></tr>`;
    }).join("");
    renderPaginacao("#key-pag", dados, carregarChaves);
  } catch (e) {
    linhaVazia("#key-tbody", 8, mensagemErro(e));
    $("#key-pag").innerHTML = "";
  }
}

function mostrarChaveNova(valor) {
  $("#key-nova-valor").textContent = valor;
  $("#key-nova").classList.remove("escondido");
}

function ligarChaves() {
  const form = $("#key-form");
  form.addEventListener("submit", async (ev) => {
    ev.preventDefault();
    const d = dadosDoForm(form);
    try {
      const { dados } = await api("POST", "/api-keys", { corpo: { nome: d.nome, nivel: d.nivel, plano: d.plano } });
      mostrarChaveNova(dados.chave);
      form.reset();
      carregarChaves(chaves.url);
    } catch (e) { avisar(mensagemErro(e), "erro"); }
  });
  $("#key-copiar").addEventListener("click", async () => {
    try {
      await navigator.clipboard.writeText($("#key-nova-valor").textContent);
      avisar("Chave copiada.");
    } catch { avisar("Não deu pra copiar automaticamente, selecione e copie.", "erro"); }
  });
  $("#key-usar").addEventListener("click", () => usarChave($("#key-nova-valor").textContent));

  // trocar o plano direto na tabela (PUT /api-keys/{id})
  $("#key-tbody").addEventListener("change", async (ev) => {
    const sel = ev.target.closest("select[data-plano]");
    if (!sel) return;
    const k = chaves.cache.get(sel.dataset.plano);
    try {
      await api("PUT", k._links.update.href, { corpo: { nome: k.nome, nivel: k.nivel, plano: sel.value, ativa: k.ativa } });
      avisar(`Chave "${k.nome}" agora está no plano ${sel.value}.`);
      carregarChaves(chaves.url);
      if (cfg.key) verificarChave();
    } catch (e) {
      sel.value = k.plano || "GRATUITO";
      avisar(mensagemErro(e), "erro");
    }
  });

  $("#key-tbody").addEventListener("click", async (ev) => {
    const b = ev.target.closest("button");
    if (!b) return;
    try {
      if (b.dataset.alternar) {
        const k = chaves.cache.get(b.dataset.alternar);
        await api("PUT", k._links.update.href, { corpo: { nome: k.nome, nivel: k.nivel, plano: k.plano, ativa: !k.ativa } });
        avisar(`Chave "${k.nome}" ${k.ativa ? "desativada" : "ativada"}.`);
        carregarChaves(chaves.url);
      } else if (b.dataset.rotacionar) {
        const k = chaves.cache.get(b.dataset.rotacionar);
        if (!confirm(`Gerar um novo valor para "${k.nome}"? O valor atual para de funcionar.`)) return;
        const { dados } = await api("POST", k._links.rotacionar.href);
        mostrarChaveNova(dados.chave);
        carregarChaves(chaves.url);
      } else if (b.dataset.excluir) {
        excluir(b.dataset.excluir, `a chave "${b.dataset.nome}"`, () => carregarChaves(chaves.url));
      }
    } catch (e) { avisar(mensagemErro(e), "erro"); }
  });
}

// =====================================================================
// RECURSOS AVANCADOS (rate limit, idempotencia, versionamento, CORS)
// =====================================================================
function resumo(r) {
  if (r instanceof ApiErro) return `${r.status} ${r.dados?.message || ""}`;
  if (r instanceof Error) return `ERRO ${r.message}`;
  return `${r.status} OK`;
}

async function tentar(fn) {
  try { return await fn(); } catch (e) { return e; }
}

function ligarTestes() {
  // ---- rate limit ----
  $("#rl-btn").addEventListener("click", async () => {
    const qtd = Math.min(100, Math.max(1, Number($("#rl-qtd").value) || 25));
    const saida = $("#rl-saida");
    saida.textContent = "Disparando...\n";
    let politica = "";
    const contagem = {};
    for (let i = 1; i <= qtd; i++) {
      const r = await tentar(() => api("GET", "/categorias?size=1"));
      const st = r.status ?? "falha";
      contagem[st] = (contagem[st] || 0) + 1;
      const h = r.headers || {};
      if (!politica && h["X-RateLimit-Policy"]) {
        politica = h["X-RateLimit-Policy"];
        saida.textContent = `X-RateLimit-Policy: ${politica}\n\n`;
      }
      saida.textContent += `#${String(i).padStart(2, "0")} → ${st}  restantes=${h["X-RateLimit-Remaining"] ?? "?"}`
        + (h["Retry-After"] ? `  Retry-After=${h["Retry-After"]}s` : "") + "\n";
    }
    saida.textContent += "\nResumo: " + Object.entries(contagem).map(([k, v]) => `${v}× ${k}`).join(", ");
    if (contagem[429] >= 5) {
      saida.textContent += "\nComo continuou insistindo depois do 429, a API aplicou a penalidade (veja o Retry-After maior).";
    }
  });

  // ---- politica de limites ----
  $("#lim-btn").addEventListener("click", async () => {
    const r = await tentar(() => api("GET", "/limites"));
    const corpo = r.dados ? { ...r.dados } : r.message;
    if (corpo && corpo._links) delete corpo._links;
    $("#lim-saida").textContent = r instanceof ApiErro ? resumo(r) : JSON.stringify(corpo, null, 2);
  });

  // ---- idempotencia ----
  $("#idem-btn").addEventListener("click", async () => {
    const saida = $("#idem-saida");
    const chave = uuid();
    const corpo = { nome: "Teste idempotência " + chave.slice(0, 6), descricao: "criada pelo teste" };
    saida.textContent = `X-Idempotency-Key: ${chave}\n\n`;
    const r1 = await tentar(() => api("POST", "/categorias", { corpo, idemKey: chave }));
    saida.textContent += `1º envio            → ${resumo(r1)}${r1.dados?.id ? `  (categoria #${r1.dados.id})` : ""}\n`;
    const r2 = await tentar(() => api("POST", "/categorias", { corpo, idemKey: chave }));
    saida.textContent += `2º envio (igual)    → ${resumo(r2)}${r2.dados?.id ? `  (categoria #${r2.dados.id})` : ""}`
      + `  replayed=${r2.headers?.["X-Idempotency-Replayed"] || "não"}\n`;
    const r3 = await tentar(() => api("POST", "/categorias", { corpo: { ...corpo, nome: corpo.nome + " (outro)" }, idemKey: chave }));
    saida.textContent += `3º envio (diferente)→ ${resumo(r3)}\n`;
    const r4 = await tentar(() => api("POST", "/categorias", { corpo, idemKey: null }));
    saida.textContent += `sem a chave         → ${resumo(r4)}\n`;
    if (r1.dados?.id && r2.dados?.id) {
      saida.textContent += r1.dados.id === r2.dados.id
        ? "\n✔ Mesmo id nas duas: a categoria foi criada uma vez só."
        : "\n✘ Ids diferentes: algo está errado.";
    }
  });

  // ---- versionamento ----
  $("#ver-btn").addEventListener("click", async () => {
    const id = Number($("#ver-id").value) || 1;
    for (const v of ["1", "2"]) {
      const r = await tentar(() => api("GET", `/produtos/${id}`, { versao: v }));
      const corpo = r.dados ? { ...r.dados } : r.message;
      if (corpo && corpo._links) delete corpo._links;
      $(`#ver-v${v}`).textContent = `X-API-Version: ${v}\n` +
        `(resposta veio como v${r.headers?.["X-API-Version"] ?? "?"})\n\n` + JSON.stringify(corpo, null, 2);
    }
  });

  // ---- seguranca ----
  $("#sec-btn").addEventListener("click", async () => {
    const saida = $("#sec-saida");
    saida.textContent = "";
    const casos = [
      ["GET /clientes sem chave", () => api("GET", "/clientes?size=1", { chave: "" })],
      ["GET /clientes chave inválida", () => api("GET", "/clientes?size=1", { chave: "ll_chave_que_nao_existe" })],
      ["GET /clientes com LEITURA", () => api("GET", "/clientes?size=1", { chave: "ll_leitura_dev_123" })],
      ["DELETE /produtos/999 com LEITURA", () => api("DELETE", "/produtos/999", { chave: "ll_leitura_dev_123" })],
      ["GET /api-keys com ESCRITA", () => api("GET", "/api-keys?size=1", { chave: "ll_escrita_dev_123" })],
      ["GET /produtos sem chave (público)", () => api("GET", "/produtos?size=1", { chave: "" })],
    ];
    for (const [nome, fn] of casos) {
      const r = await tentar(fn);
      saida.textContent += `${nome.padEnd(34)} → ${resumo(r)}\n`;
    }
  });

  // ---- cors ----
  $("#cors-origem").textContent = location.origin;
  $("#cors-api").textContent = cfg.base;
  $("#cors-btn").addEventListener("click", async () => {
    const r = await tentar(() => api("GET", "/categorias?size=1"));
    $("#cors-saida").textContent = r instanceof Error && !(r instanceof ApiErro)
      ? `Bloqueado/sem resposta: ${r.message}\nAbra o console (F12) para ver a mensagem de CORS do navegador.`
      : `Acesso permitido (${r.status}). A origem ${location.origin} está liberada na API.`;
  });
}

// =====================================================================
// MINHA CONTA: login, cadastro e chave de API pessoal
// =====================================================================
const conta = { token: lerSessao(), usuario: null };

function lerSessao() {
  try { return sessionStorage.getItem("ll.sessao") || ""; } catch { return ""; }
}
function salvarSessao(t) {
  try { t ? sessionStorage.setItem("ll.sessao", t) : sessionStorage.removeItem("ll.sessao"); } catch { /* so memoria */ }
}

// as rotas /auth nao usam X-API-Key (chave: "") nem idempotencia (idemKey: null)
const auth = (metodo, caminho, corpo) =>
  api(metodo, caminho, { corpo, chave: "", idemKey: null, sessao: conta.token || undefined });

// ---------- tela de entrada (login / cadastro) x painel ----------
function mostrarEntrada(qual = "login") {
  document.body.classList.remove("logado");
  $("#login-form").classList.toggle("escondido", qual !== "login");
  $("#cadastro-form").classList.toggle("escondido", qual !== "cadastro");
  $("#entrada-base").value = cfg.base;
  $(qual === "login" ? "#login-form input[name=email]" : "#cadastro-form input[name=nome]")?.focus();
}

function mostrarPainel() {
  document.body.classList.add("logado");
  $("#st-usuario").textContent = conta.usuario ? `👤 ${conta.usuario.nome}` : "";
  abrirAba("produtos");
}

// a chave pessoal fica guardada no navegador, separada por usuario.
// ao entrar: se a chave guardada ainda e a da conta, usa; se a conta nao tem chave, cria uma na hora.
const chaveGuardada = (u) => lerStorage("ll.key.u" + u.id) || "";

async function garantirChave() {
  const u = conta.usuario;
  const k = u.chave;
  const guardada = chaveGuardada(u);
  if (k && guardada && guardada.startsWith((k.prefixo || "").replace("...", ""))) {
    definirChave(guardada);
    return;
  }
  if (k && !k.ativa) {
    definirChave("");
    avisar("Sua chave de API foi desativada por um administrador. Você só consegue ver o que é público.", "erro");
    return;
  }
  // sem chave (ou chave de outro navegador, cujo valor nao da pra recuperar): gera o valor novo
  const { dados } = await auth("POST", "/auth/minha-chave");
  salvarStorage("ll.key.u" + u.id, dados.chave);
  u.chave = { id: dados.id, prefixo: dados.prefixo, nivel: dados.nivel, plano: dados.plano, ativa: true, ultimoUso: null };
  definirChave(dados.chave);
}

// login feito: guarda o token, garante a chave e abre o painel
async function entrar(dadosLogin) {
  conta.token = dadosLogin.token; salvarSessao(dadosLogin.token);
  conta.usuario = dadosLogin.usuario;
  try {
    await garantirChave();
  } catch (e) {
    avisar("Entrou, mas não deu pra preparar a chave de API: " + mensagemErro(e), "erro");
  }
  mostrarPainel();
}

// ao abrir a pagina: se ainda tem sessao valida, vai direto pro painel
async function retomarSessao() {
  if (!conta.token) { mostrarEntrada(); return; }
  try {
    const { dados } = await auth("GET", "/auth/me");
    conta.usuario = dados;
    if (cfg.key) { verificarChave(); } else { await garantirChave(); }
    mostrarPainel();
  } catch {
    conta.token = ""; salvarSessao(""); conta.usuario = null;
    mostrarEntrada();
  }
}

async function sair() {
  try { await auth("POST", "/auth/logout"); } catch { /* sai mesmo assim */ }
  conta.token = ""; salvarSessao(""); conta.usuario = null;
  cfg.key = ""; $("#cfg-key").value = ""; salvarStorage("ll.key", "");
  cfg.nivel = null;
  $("#conta-nova").classList.add("escondido");
  mostrarEntrada();
}

function renderConta() {
  if (!conta.usuario) { mostrarEntrada(); return; }
  const u = conta.usuario;
  $("#conta-nome").textContent = u.nome;
  $("#conta-email").textContent = u.email;
  const k = u.chave;
  $("#conta-chave").innerHTML = k
    ? `<dl class="chave-info">
        <dt>Chave</dt><dd><code>${esc(k.prefixo)}</code></dd>
        <dt>Nível</dt><dd><span class="selo">${esc(k.nivel)}</span></dd>
        <dt>Plano</dt><dd><span class="selo">${esc(k.plano)}</span></dd>
        <dt>Situação</dt><dd><span class="selo ${k.ativa ? "ok" : "nao"}">${k.ativa ? "ativa" : "desativada por um administrador"}</span></dd>
        <dt>Último uso</dt><dd>${dataHora(k.ultimoUso)}</dd>
      </dl>`
    : `<p class="dica">Você ainda não tem chave de API.</p>`;
  $("#conta-gerar").textContent = k ? "Gerar nova chave" : "Criar minha chave";
  $("#conta-gerar").disabled = !!(k && !k.ativa);
  $("#conta-excluir").classList.toggle("escondido", !k);
  $("#conta-copiar-atual").classList.toggle("escondido", !k);
}

async function carregarConta() {
  if (!conta.token) { mostrarEntrada(); return; }
  try {
    const { dados } = await auth("GET", "/auth/me");
    conta.usuario = dados;
    renderConta();
  } catch (e) {
    // sessao expirou ou a API reiniciou: volta pro login
    if (e instanceof ApiErro && e.status === 401) {
      conta.token = ""; salvarSessao(""); conta.usuario = null;
      avisar("Sua sessão expirou. Entre de novo.", "erro");
      mostrarEntrada();
    } else {
      avisar(mensagemErro(e), "erro");
    }
  }
}

function ligarConta() {
  // trocar entre "Entrar" e "Cadastre-se"
  $$("[data-ir]").forEach((b) => b.addEventListener("click", () => {
    $("#entrada-aviso").classList.add("escondido");
    mostrarEntrada(b.dataset.ir);
  }));

  // a URL da API tambem pode ser trocada na tela de entrada (ex.: localhost ou Render)
  const lerUrlEntrada = () => {
    const v = $("#entrada-base").value.trim().replace(/\/$/, "");
    if (v) { cfg.base = v; salvarStorage("ll.base", v); $("#cfg-base").value = v; }
  };

  $("#login-form").addEventListener("submit", async (ev) => {
    ev.preventDefault();
    lerUrlEntrada();
    const d = dadosDoForm(ev.target);
    const botao = ev.target.querySelector("button:not([type=button])");
    botao.disabled = true;
    try {
      const { dados } = await auth("POST", "/auth/login", { email: d.email, senha: d.senha });
      ev.target.reset();
      await entrar(dados);
      avisar(`Bem-vinda(o), ${dados.usuario.nome}!`);
    } catch (e) {
      avisar(e instanceof ApiErro && e.status === 401 ? "E-mail ou senha incorretos." : mensagemErro(e), "erro");
    } finally { botao.disabled = false; }
  });

  $("#cadastro-form").addEventListener("submit", async (ev) => {
    ev.preventDefault();
    lerUrlEntrada();
    const d = dadosDoForm(ev.target);
    const botao = ev.target.querySelector("button:not([type=button])");
    botao.disabled = true;
    try {
      await auth("POST", "/auth/cadastro", { nome: d.nome, email: d.email, senha: d.senha });
      // ja entra direto depois de criar a conta
      const { dados } = await auth("POST", "/auth/login", { email: d.email, senha: d.senha });
      ev.target.reset();
      await entrar(dados);
      avisar(`Conta criada! Bem-vinda(o), ${dados.usuario.nome}. Sua chave de API já está em uso.`);
    } catch (e) {
      avisar(e instanceof ApiErro && e.status === 409 ? "Já existe uma conta com esse e-mail. Faça o login."
        : mensagemErro(e), "erro");
    } finally { botao.disabled = false; }
  });

  $("#conta-gerar").addEventListener("click", async () => {
    if (conta.usuario?.chave && !confirm("Gerar uma chave nova? A chave atual para de funcionar na hora.")) return;
    try {
      const { dados } = await auth("POST", "/auth/minha-chave");
      $("#conta-nova-valor").textContent = dados.chave;
      $("#conta-nova").classList.remove("escondido");
      salvarStorage("ll.key.u" + conta.usuario.id, dados.chave);
      // atualiza a tela com o que veio na resposta (sem outra requisicao: o limite sem chave e pequeno)
      conta.usuario.chave = { id: dados.id, prefixo: dados.prefixo, nivel: dados.nivel, plano: dados.plano,
        ativa: true, ultimoUso: conta.usuario.chave?.ultimoUso || null };
      renderConta();
    } catch (e) {
      if (e instanceof ApiErro && e.status === 401) { conta.token = ""; salvarSessao(""); conta.usuario = null; mostrarEntrada(); }
      avisar(mensagemErro(e), "erro");
    }
  });

  // copia a chave que ja existe. a API nunca devolve o valor de novo (so guarda o hash),
  // entao o valor vem do que ficou salvo neste navegador quando a chave foi criada
  $("#conta-copiar-atual").addEventListener("click", async () => {
    const valor = chaveGuardada(conta.usuario);
    if (!valor) {
      avisar("Esta chave foi criada em outro navegador, então o valor não está salvo aqui. "
        + "Clique em \"Gerar nova chave\" para ter um valor novo.", "erro");
      return;
    }
    try {
      await navigator.clipboard.writeText(valor);
      avisar("Chave copiada.");
    } catch {
      $("#conta-nova-valor").textContent = valor;
      $("#conta-nova").classList.remove("escondido");
      avisar("Não deu pra copiar automaticamente: a chave apareceu abaixo, selecione e copie.", "erro");
    }
  });

  // DELETE /auth/minha-chave: a chave para de funcionar na hora e a conta fica sem chave
  $("#conta-excluir").addEventListener("click", async () => {
    if (!confirm("Excluir a sua chave de API? Ela para de funcionar na hora e o painel fica só com o que é público.")) return;
    try {
      await auth("DELETE", "/auth/minha-chave");
      const u = conta.usuario;
      const guardada = chaveGuardada(u);
      salvarStorage("ll.key.u" + u.id, "");
      u.chave = null;
      $("#conta-nova").classList.add("escondido");
      if (guardada && cfg.key === guardada) definirChave("");
      renderConta();
      avisar("Chave excluída. Se precisar, clique em \"Criar minha chave\" para gerar outra.");
    } catch (e) {
      if (e instanceof ApiErro && e.status === 401) { conta.token = ""; salvarSessao(""); conta.usuario = null; mostrarEntrada(); }
      avisar(mensagemErro(e), "erro");
    }
  });

  $("#conta-copiar").addEventListener("click", async () => {
    try {
      await navigator.clipboard.writeText($("#conta-nova-valor").textContent);
      avisar("Chave copiada.");
    } catch { avisar("Não deu pra copiar automaticamente, selecione e copie.", "erro"); }
  });
  $("#conta-usar").addEventListener("click", () => {
    usarChave($("#conta-nova-valor").textContent);
    avisar("Pronto! O painel agora usa a sua chave.");
    abrirAba("produtos");
  });

  $("#conta-sair").addEventListener("click", sair);
  $("#topo-sair").addEventListener("click", sair);
}

// =====================================================================
// ABAS, CONFIGURACAO E INICIO
// =====================================================================
const CARREGAR_ABA = {
  conta: carregarConta,
  produtos: () => { carregarOpcoesCategorias(); carregarProdutos(produtos.url); },
  categorias: () => carregarCategorias(categorias.url),
  clientes: () => carregarClientes(clientes.url),
  pedidos: () => { carregarOpcoesPedido(); carregarPedidos(pedidos.url); },
  chaves: () => carregarChaves(chaves.url),
  testes: () => { $("#cors-api").textContent = cfg.base; },
  log: renderLog,
};

let abaAtual = "produtos";
function abrirAba(nome) {
  abaAtual = nome;
  $$(".abas button").forEach((b) => b.classList.toggle("ativa", b.dataset.aba === nome));
  $$(".aba").forEach((s) => s.classList.toggle("escondido", s.id !== "aba-" + nome));
  CARREGAR_ABA[nome]?.();
}

// so troca a chave (sem recarregar a aba): usado no login, antes do painel abrir
function definirChave(valor) {
  cfg.key = valor;
  $("#cfg-key").value = valor;
  salvarStorage("ll.key", valor);
  verificarChave();
}

function usarChave(valor) {
  cfg.key = valor;
  $("#cfg-key").value = valor;
  salvarStorage("ll.key", valor);
  verificarChave();
  abrirAba(abaAtual);
}

// contador de caracteres embaixo de todo campo de texto que tem limite (maxlength)
function ligarContadores() {
  const campos = $$("form.cartao input[maxlength], form.cartao textarea[maxlength]");
  const atualizar = (campo) => {
    const max = Number(campo.maxLength);
    campo.nextElementSibling.textContent = `${campo.value.length}/${max}`;
    campo.nextElementSibling.classList.toggle("cheio", campo.value.length >= max);
  };
  campos.forEach((campo) => {
    const cont = document.createElement("div");
    cont.className = "contador";
    campo.insertAdjacentElement("afterend", cont);
    campo.addEventListener("input", () => atualizar(campo));
    atualizar(campo);
  });
  // "Editar", "Cancelar" e o reset depois de salvar mudam o valor sem digitar: atualiza tudo a cada clique/envio
  const todos = () => setTimeout(() => campos.forEach(atualizar));
  document.addEventListener("click", todos);
  document.addEventListener("submit", todos);
  document.addEventListener("reset", todos, true);
}

function iniciar() {
  $("#cfg-base").value = cfg.base;
  $("#cfg-key").value = cfg.key;
  $("#cfg-versao").value = cfg.versao;

  $("#form-config").addEventListener("submit", (ev) => {
    ev.preventDefault();
    cfg.base = $("#cfg-base").value.trim().replace(/\/$/, "");
    cfg.versao = $("#cfg-versao").value;
    salvarStorage("ll.base", cfg.base);
    salvarStorage("ll.versao", cfg.versao);
    usarChave($("#cfg-key").value.trim());
    avisar("Configuração salva.");
  });
  $("#cfg-versao").addEventListener("change", () => {
    cfg.versao = $("#cfg-versao").value;
    salvarStorage("ll.versao", cfg.versao);
    abrirAba(abaAtual);
  });
  $$("[data-demo]").forEach((b) => b.addEventListener("click", () => usarChave(b.dataset.demo)));
  $$(".abas button").forEach((b) => b.addEventListener("click", () => abrirAba(b.dataset.aba)));
  $("#log-limpar").addEventListener("click", () => { log.length = 0; renderLog(); });

  ligarProdutos();
  ligarCategorias();
  ligarClientes();
  ligarPedidos();
  ligarChaves();
  ligarTestes();
  ligarConta();
  ligarContadores();

  // a primeira tela e sempre o login (ou o painel direto, se a sessao ainda vale)
  retomarSessao();
}

iniciar();
