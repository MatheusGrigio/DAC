# Relatório de integração — Frontend × Backend (BANTADS)

**Primeira análise:** código de `frontend/` contra o contrato que o backend entrega (73 testes de `backend/contract-tests` + HTTPie E2E).

**Revisão:** 28/10/2026, depois dos commits de frontend de 27/10/2026:

| Commit | Mensagem |
|--------|----------|
| `f743536` | polling de jobs assíncronos (R9) |
| `51510f3` | Adicionando depósito e `garantir` no `ContaService` |
| `e97c746` | validações de autocadastro e integração das telas |
| `77b66a9` | merge do PR #20 |

O frontend continua falando só com o Gateway em `http://localhost:3000`.

---

## O que esta leva mudou

| Item anterior | Situação agora |
|---------------|----------------|
| **2.1** status `REJEITADA` no lugar de `NAO_APROVADA` | **Corrigido na tela.** O badge e o texto usam `NAO_APROVADA`. O tipo ainda lista `REJEITADA` junto (resíduo, ver abaixo). |
| **2.2** aprovação sem poll do job | **Corrigido.** `POST /aprovacao` encadeia `JobService.aguardarConclusao`. `FALHA` mostra `job.erro` e a lista só recarrega depois do job. |
| **2.4** depósito stub | **Corrigido.** `deposito.ts` chama `POST /contas/{numero}/deposito` com `valor.toFixed(2)` e espera o saldo via `aguardarSaldo`. |
| **2.5** saldo ausente no dashboard do cliente | **Corrigido.** O card "Sua conta" mostra número e saldo (`dashboard.html`). |
| **2.3** transferência não chama a API | **Ainda aberto.** `transferencia.ts` segue só com `console.log`. |
| **2.4** saque stub | **Ainda aberto.** `saque.html` continua `<p>saque works!</p>`. Não existe `ContaService.sacar`. |
| **2.6** botões ignoram `_links` | **Ainda aberto.** Os quatro cards do cliente e Aprovar/Recusar do gerente continuam fixos. O computed `operacoes` não é usado no HTML. O card de extrato aparece (estava no HTML estático). |
| **3.1** CRUD de gerentes (R12–R15) | **Ainda aberto.** Nenhuma rota nem chamada HTTP. |
| **3.2** dados cadastrais do cliente | **Ainda aberto.** |
| **3.3** busca de clientes no servidor | **Ainda aberto.** O formulário só copia `busca` para `filtro` e filtra a lista já carregada. |
| **3.4** `GET /solicitacoes/{cpf}` | **Ainda aberto.** |
| **4.1** URL fixa no `AuthService` | **Ainda aberto.** |
| **4.2** interceptor sem 401 | **Ainda aberto.** |
| **4.3** `complemento` vazio | **Ainda aberto.** As validações novas não convertem `""` em `null`. |
| **4.4** `mensagemDeErro` ignora `message` | **Ainda aberto.** |
| **4.5** fuso do extrato | **Ainda aberto.** |

Nenhum fluxo que já batia com o contrato foi desfeito. Login, autocadastro, extrato, listagem, rejeição e relatório R16 seguem alinhados. O relatório R16 passou a usar o mesmo `JobService` do R9.

---

## Resíduos desta leva

Não quebram o contrato com o backend de hoje. Vale limpar antes de copiar o padrão para saque, transferência e CRUD de gerentes.

### R1 — `REJEITADA` ficou no tipo

`solicitacao.model.ts` agora é:

```ts
export type StatusSolicitacao = 'PENDENTE' | 'APROVADA' | 'NAO_APROVADA' | 'REJEITADA';
```

O backend só devolve `PENDENTE`, `APROVADA` e `NAO_APROVADA`. O badge trata `NAO_APROVADA` (texto "Não aprovado", classe vermelha). `REJEITADA` entra no `ngClass` vermelho, mas o texto cai no ramo "Pendente":

```html
{{ s.status === 'NAO_APROVADA' ? 'Não aprovado' : (s.status === 'APROVADA' ? 'Aprovado' : 'Pendente') }}
```

Com a API atual o rótulo fica certo. O valor `REJEITADA` pode fazer alguém voltar a comparar o status errado. Remover `REJEITADA` do tipo e do `ngClass`.

### R2 — `garantir()` mostra saldo em cache

`ContaService.garantir(cpf)` devolve a conta que já está no signal, sem novo `GET`, quando o CPF é o mesmo. A tela de depósito usa isso no `ngOnInit`.

O depósito em si, depois do 201, rebusca com `aguardarSaldo` e atualiza o signal. O saldo fica defasado se o usuário abrir Depósito com uma conta já carregada e o saldo tiver mudado fora dessa tela (saque ou transferência, quando existirem). Para o depósito isolado, o fluxo fecha.

### R3 — poll da aprovação não é cancelado ao sair da tela

`relatorio.ts` usa `takeUntilDestroyed`. `dashboard.ts` do gerente não. Se o gerente sai no meio dos 60 s de poll, o `subscribe` continua e ainda escreve `error` / `mensagemSucesso`. O relatório distingue `TimeoutError` ("O relatório demorou…"). A aprovação, no timeout, cai na frase genérica "Erro ao processar aprovação do cliente."

---

## 1. O que está alinhado com o backend

| Área | Detalhe |
|------|---------|
| **Base URL** | `environment.ts` exporta `apiUrl: 'http://localhost:3000'`. Serviços de conta, solicitação, job, relatório e listagem de clientes usam esse valor. Login e logout não (item 4.1). |
| **Login** | `POST /login` com `{ email, senha }`. Resposta `{ auth, token, tipo, usuario }` mapeada. Roteamento para `/cliente/dashboard` ou `/gerente/dashboard`. Botão mostrar/ocultar senha é só da tela. |
| **Token JWT** | `auth.interceptor.ts` envia `x-access-token`. |
| **Logout** | `POST /logout` e limpeza do `localStorage`. |
| **Autocadastro (R1)** | `POST /solicitacoes`. `salario` com `Decimal.toFixed(2)`. CPF, telefone e CEP só dígitos. Nome, logradouro, número, cidade e UF barram campo só com espaços. Mensagem de sucesso deixa claro que a senha chega por e-mail depois da aprovação. |
| **Consulta da conta (R3A)** | `GET /clientes/{cpf}/conta`. `saldo` → `Decimal`, `dataCriacao` → `DateTime`. |
| **Dashboard do cliente** | Número da conta e saldo no card "Sua conta", com carregando e "Tentar novamente". |
| **Depósito (R4)** | `POST /contas/{numero}/deposito` com `{ valor: "<com duas casas>" }`. Máscara de centavos (`centavosParaDecimal`). 400 e 403 com texto próprio. Depois do 201, poll de até 6 leituras até o saldo esperado. A resposta da operação não é usada como saldo. |
| **Extrato (R7)** | `GET /contas/{numero}/extrato?inicio=&fim=` em `yyyy-MM-dd`, teto de 365 dias, linha do tempo com `saldoAbertura`. |
| **Listar clientes (R11)** | `GET /clientes`. Campos `cpf`, `nome`, `cidade`, `estado`, `saldo`. Botão "Tentar novamente" chama de novo o GET sem filtro. |
| **Listar solicitações (R8A)** | `GET /solicitacoes`, inclusive `_links` no model. A tela ainda decide o botão pelo `status` (item 2.6). |
| **Aprovação (R9)** | `POST /solicitacoes/{cpf}/aprovacao` → `jobId` → `GET /jobs/{id}/status` a cada 1 s, até 60 s. `FALHA` exibe `erro` (ex.: "E-mail já cadastrado", "Solicitação não encontrada ou já processada") e recarrega a lista. `CONCLUIDO` mostra sucesso e recarrega. Botão fica "Processando..." no CPF da linha. |
| **Rejeição (R10)** | `POST /solicitacoes/{cpf}/rejeicao` com `{ motivo }` via `prompt`. Síncrono, como o contrato. |
| **Relatório (R16)** | 202 → `aguardarConclusao` → `GET /jobs/{id}/result`. Timeout com mensagem própria. |
| **Jobs** | `JobService` em `GET /jobs/{id}/status`, `GET /jobs/{id}/result` e `GET /{dominio}/{resourceId}`. O último método existe e ainda não é usado pela aprovação (a lista recarregada já reflete `APROVADA` / `NAO_APROVADA`). |
| **Stack** | Angular, `decimal.js`, `luxon`, signals. |

---

## 2. O que ainda quebra a integração

### 2.3 — Transferência (R6) não chama a API

`transferencia.ts` monta o payload, faz `console.log` e mostra "Transferência solicitada com sucesso!" sem `POST`.

Dois formatos continuam incompatíveis com o contrato:

- `valor: valorDecimal.toString()` produz `"100"` em vez de `"100.00"`. O Gateway exige `^\d+\.\d{2}$`. O depósito já usa `toFixed(2)`.
- `contaDestino` valida `^[0-9]+$`. `"950"` passa e a conta do seed é `"0950"` (4 caracteres). O backend responde 422.

Chamada esperada:

```
POST /contas/{numeroDaContaOrigem}/transferencia
{ "contaDestino": "0950", "valor": "100.00" }
```

O número de origem está em `contaService.conta()?.numero`. O 201 não traz saldo. Em seguida, reconsultar `GET /clientes/{cpf}/conta` (`aguardarSaldo`, como o depósito).

### 2.4 — Saque (R5) continua stub

`saque.html` é `<p>saque works!</p>`. `saque.ts` não tem lógica. `ContaService` não tem `sacar`.

```
POST /contas/{numero}/saque
{ "valor": "10.00" }
```

Saldo insuficiente: 422 `{ status, erro, mensagem: "Saldo insuficiente para a operação" }`. `mensagemDeErro` já lê `mensagem`. Depois do 201, reconsultar a conta. O depósito é o modelo a copiar (valor com duas casas, poll do saldo, mensagens de 400/403).

### 2.6 — Botões ignoram `_links`

O dashboard do cliente calcula as operações a partir dos links:

```ts
protected readonly operacoes = computed(() =>
  OPERACOES.filter((op) => !!this.conta()?.links[op.rel]),
);
```

O HTML não usa `operacoes()`. São quatro cards com `routerLink` fixo (depósito, saque, transferência, extrato), com ou sem o link na conta. `extrato` está com `disponivel: false` nessa lista, e isso não muda o HTML: o card de extrato aparece.

No gerente, Aprovar e Recusar usam `*ngIf="s.status === 'PENDENTE'"` em vez de `s._links?.aprovacao` e `s._links?.rejeicao`. O backend tira esses links quando a solicitação já foi processada.

---

## 3. O que o backend já entrega e o front ainda não tem

### 3.1 — CRUD de gerentes (R12–R15)

Há models (`GerenteResponse`, `GerenteInput`, `GerenteAtualizacao`). Não há `gerente.service.ts`, tela nem rota em `app.routes.ts`. O menu do gerente só tem "Consultar clientes" e "Relatório de clientes".

| Operação | Endpoint | Contrato |
|----------|----------|----------|
| Listar (R12) | `GET /gerentes` | Lista com `_links.criacao` |
| Consultar (CAD-02) | `GET /gerentes/{cpf}` | Cache de 5 min no Gateway |
| Inserir (R13) | `POST /gerentes` `{ cpf, nome, email, telefone, senha }` | 202 + job. `JobService.aguardarConclusao` já serve |
| Atualizar (R14) | `PUT /gerentes/{cpf}` `{ nome, telefone }` | 200 síncrono. `email` e `cpf` fora do body. Se forem enviados: 400 "CPF e e-mail são imutáveis" |
| Remover (R15) | `DELETE /gerentes/{cpf}` | O próprio CPF: 403 síncrono, sem job. Outro gerente: 202; `GET /jobs/{id}/result` devolve `{ mensagem: "Gerente removido; N contas transferidas para …" }` |

`atualizacao` e `remocao` só aparecem se estiverem em `_links`. O gerente autenticado não recebe `remocao` no próprio registro.

### 3.2 — Dados cadastrais do cliente (CAD-01)

`GET /clientes/{cpf}` devolve cadastro (sem saldo; saldo é a conta). Não há tela nem item de menu.

```json
{
  "cpf": "12912861012",
  "nome": "Catharyna",
  "email": "cli1@bantads.com.br",
  "telefone": "41999990001",
  "salario": "10000.00",
  "endereco": {
    "logradouro": "...",
    "numero": "...",
    "complemento": null,
    "cep": "...",
    "cidade": "...",
    "uf": "..."
  },
  "_links": { "self": {}, "conta": {} }
}
```

### 3.3 — Busca de clientes ainda é local

`clientes.html` faz `(ngSubmit)="filtro = busca"`. `clientesFiltrados` filtra nome/CPF e ordena no browser. O GET é sempre `/clientes`, sem query.

O contrato é `GET /clientes?busca=Cat`: filtro e ordenação pt-BR no servidor (MS Cliente + saldo vindo da conta). O campo deve disparar esse GET. A ordenação local deixa de ser necessária.

### 3.4 — Solicitação individual (R8B)

`GET /solicitacoes/{cpf}` devolve a solicitação com endereço completo. A tela do gerente só usa a lista. Útil se o gerente precisar ver o endereço antes de aprovar ou recusar.

---

## 4. Problemas técnicos que continuam

### 4.1 — `AuthService` não usa `environment.apiUrl`

`auth.service.ts`:

```ts
private readonly API_GATEWAY = 'http://localhost:3000';
```

Login e logout ficam nesse host mesmo que `environment.apiUrl` mude. Os outros serviços já leem o environment.

### 4.2 — Interceptor não encerra sessão no 401

O interceptor só coloca o token. 401 (token expirado, logout em outra aba, `/reboot`) chega na tela da chamada. `localStorage` e a rota atual permanecem. No `catchError` do interceptor, 401 deve chamar `authService.encerrarSessaoLocal()`.

### 4.3 — `complemento` vazio segue como `""`

`autocadastro.ts` inicia `complemento: ''` e o `enviar` espalha o objeto de endereço inteiro. O catálogo (`TX-R1.md`) usa `"complemento": null`. As validações novas cobrem nome, CPF, telefone, salário, CEP, logradouro, número, cidade e UF. Complemento ficou de fora.

No envio: `complemento: this.cadastro.complemento.trim() || null`.

### 4.4 — `mensagemDeErro` não lê o 401 do Gateway

Corpo de autenticação: `{ "auth": false, "message": "Login inválido!" }` (campo `message`). `erro-http.ts` só lê `error.mensagem`. O login trata 401 com texto fixo no `AuthService`, então a tela de login funciona. Nas outras telas, "Falha ao autenticar o token." e "Token não fornecido." caem no texto padrão.

```ts
const mensagem = e?.error?.mensagem ?? e?.error?.message;
```

### 4.5 — Extrato usa o fuso do browser

`extrato.ts` usa `DateTime.now()` sem zona. O negócio é `America/Sao_Paulo`, data naive sem offset. Intervalo padrão e o corte do dia podem divergir do servidor.

`DateTime.now().setZone('America/Sao_Paulo')`.

---

## 5. Prioridade agora

### Crítico — o usuário não consegue concluir a operação

| # | Problema | Onde |
|---|----------|------|
| 2.3 | Transferência não chama a API; valor e conta destino fora do formato | `transferencia.ts` |
| 2.4 | Saque é stub; falta `sacar` no serviço | `saque.ts`, `saque.html`, `conta.service.ts` |

### Alto — exigido pelo enunciado, backend pronto

| # | Problema | Onde |
|---|----------|------|
| 3.1 | CRUD de gerentes (R12–R15), jobs no inserir e no remover | serviço + telas + rotas |
| 2.6 | Cards e Aprovar/Recusar ignoram `_links` | `cliente/dashboard/dashboard.html`, `gerente/dashboard/dashboard.html` |
| 3.2 | Dados cadastrais do cliente | tela + rota |

### Médio

| # | Problema | Onde |
|---|----------|------|
| 4.2 | 401 não derruba a sessão | `auth.interceptor.ts` |
| 4.1 | URL fixa no login/logout | `auth.service.ts` |
| 3.3 | Busca de clientes só no browser | `clientes.ts`, `clientes.html` |
| 4.3 | `complemento: ""` | `autocadastro.ts` |

### Baixo

| # | Problema |
|---|----------|
| R1 | Tirar `REJEITADA` do tipo e do badge |
| R2 | `garantir()` sem reconsulta |
| R3 | Cancelar o poll da aprovação ao sair; mensagem de timeout |
| 4.4 | `mensagemDeErro` também ler `message` |
| 4.5 | Fuso `America/Sao_Paulo` no extrato |
| — | `package.json` com `name: "hello-world"` |
| — | `pages/admin` e `pages/home` sem rota |
