# Catálogo de contratos JSON — BANTADS API Gateway

Fonte canônica: [`docs/swagger_bantads.md`](docs/swagger_bantads.md).  
Tutoriais: [`transacoes/`](transacoes/00-GERAL.md) · HTTPie: [`httpie/`](httpie/00-GENERAL-INFO.md).  
Planos: [`plano-entregas-frontend.md`](plano-entregas-frontend.md) · [`plano-entregas-backend.md`](plano-entregas-backend.md).

O Cursor **não abre** links com `#âncora` (`arquivo.md#secao`): trata o hash como nome de arquivo e mostra *file not found*.  
Cada transação está em um arquivo em [`json-catalog/`](json-catalog/). Clique no TX — o editor abre request e response daquela operação.

**Convenções globais:**
- Dinheiro sempre `string` `^\d+\.\d{2}$` — `"800.00"`, nunca `800` ou `"800,00"`.
- CPF: string 11 dígitos sem pontuação — `"12912861012"`.
- Número de conta: string 4 dígitos — `"0950"` (zero à esquerda preservado).
- Datas: ISO 8601 **sem** offset — `"2026-08-18T16:50:00"`. Queries: `YYYY-MM-DD`.
- Token: header **`x-access-token`** (não `Authorization: Bearer`).
- Dois envelopes de erro: `{ auth, message }` para 401; `{ status, erro, mensagem }` para todo o resto.
- `_links` ausentes em: login, logout (204), reboot, health, jobs (202/status/result), linhas do relatório.

---

## Índice (um arquivo por transação)

| TX | Conteúdo |
|---|---|
| [TX-INFRA-01](json-catalog/TX-INFRA-01.md) | Health check |
| [TX-INFRA-02](json-catalog/TX-INFRA-02.md) | Reboot |
| [TX-R2A](json-catalog/TX-R2A.md) | Login |
| [TX-R2B](json-catalog/TX-R2B.md) | Logout |
| [TX-R1](json-catalog/TX-R1.md) | Autocadastro |
| [TX-R8A](json-catalog/TX-R8A.md) | Listar solicitações |
| [TX-R8B](json-catalog/TX-R8B.md) | Consultar solicitação |
| [TX-R9](json-catalog/TX-R9.md) | Aprovar cliente (SAGA) |
| [TX-R10](json-catalog/TX-R10.md) | Rejeitar cliente |
| [TX-CAD-01](json-catalog/TX-CAD-01.md) | Consultar cliente |
| [TX-R11](json-catalog/TX-R11.md) | Listar clientes (Composition) |
| [TX-R3A](json-catalog/TX-R3A.md) | Conta por CPF |
| [TX-R3B](json-catalog/TX-R3B.md) | Conta por número |
| [TX-R4](json-catalog/TX-R4.md) | Depósito |
| [TX-R5](json-catalog/TX-R5.md) | Saque |
| [TX-R6](json-catalog/TX-R6.md) | Transferência |
| [TX-R7](json-catalog/TX-R7.md) | Extrato |
| [TX-R12](json-catalog/TX-R12.md) | Listar gerentes (Composition) |
| [TX-CAD-02](json-catalog/TX-CAD-02.md) | Consultar gerente |
| [TX-R13](json-catalog/TX-R13.md) | Inserir gerente (SAGA) |
| [TX-R14](json-catalog/TX-R14.md) | Atualizar gerente |
| [TX-R15](json-catalog/TX-R15.md) | Remover gerente (SAGA) |
| [TX-R16](json-catalog/TX-R16.md) | Relatório de clientes |
| [TX-JOB-01](json-catalog/TX-JOB-01.md) | Status do job |
| [TX-JOB-02](json-catalog/TX-JOB-02.md) | Resultado inline do job |

---

## Resumo de envelopes de erro

| Situação | Status | Corpo |
|---|---|---|
| Body malformado | 400 | `{ "status": 400, "erro": "Bad Request", "mensagem": "Requisição malformada" }` |
| Token ausente | 401 | `{ "auth": false, "message": "Token não fornecido." }` |
| Token inválido/sessão encerrada | 401 | `{ "auth": false, "message": "Falha ao autenticar o token." }` |
| Credenciais erradas ou inativo | 401 | `{ "auth": false, "message": "Login inválido!" }` |
| Perfil sem permissão ou posse | 403 | `{ "status": 403, "erro": "Forbidden", "mensagem": "Acesso negado" }` |
| Auto-remoção (R15) | 403 | `{ "status": 403, "erro": "Forbidden", "mensagem": "Não é permitido remover a si mesmo" }` |
| Recurso não encontrado | 404 | `{ "status": 404, "erro": "Not Found", "mensagem": "..." }` |
| Job expirado | 404 | `{ "status": 404, "erro": "Not Found", "mensagem": "Job inexistente ou expirado" }` |
| Conflito de estado | 409 | `{ "status": 409, "erro": "Conflict", "mensagem": "..." }` |
| Regra de negócio síncrona | 422 | `{ "status": 422, "erro": "Unprocessable Entity", "mensagem": "..." }` |
| Job aceito (SAGA/assíncrono) | 202 | `{ "jobId": "uuid", "status": "PENDENTE" }` + header `Location` |
| MS fora / timeout | 502/504 | gerado pelo Gateway |

---

## Resumo de `_links` por recurso

| Recurso | Rels possíveis | Condição |
|---|---|---|
| Solicitação `PENDENTE` | `self`, `aprovacao`, `rejeicao` | sempre |
| Solicitação `APROVADA`/`NAO_APROVADA` | `self` | sempre |
| Lista de solicitações | `self` | inclui query string |
| Cliente (TX-CAD-01) | `self`, `conta` | sempre |
| Lista de clientes (R11) | `self`, `conta` por item | sempre |
| Envelope lista R11 | `self` | com ou sem `?busca=` |
| Conta — CLIENTE dono | `self`, `cliente`, `deposito`, `saque`, `transferencia`, `extrato` | sempre |
| Conta — GERENTE | `self`, `cliente` | `deposito`/`saque`/`transferencia`/`extrato` removidos |
| Extrato | `self`, `conta` | sempre |
| Operação (depósito/saque/transf.) | `conta`, `extrato` | sem `self` |
| Gerente ativo — não o logado | `self`, `atualizacao`, `remocao` | sempre |
| Gerente ativo — o próprio logado | `self`, `atualizacao` | `remocao` removido |
| Gerente inativo | `self` | sem `atualizacao`/`remocao` |
| Lista de gerentes (R12) | `self`, `criacao` no envelope | gerentes individuais conforme acima |
| Login | _(sem `_links`)_ | exceção |
| Logout (204) | _(sem `_links`)_ | exceção |
| Jobs (202/status/result) | _(sem `_links`)_ | exceção |
| `/health` | _(sem `_links`)_ | exceção |
| `/reboot` | _(sem `_links`)_ | exceção |
| Relatório (result R16) | _(sem `_links`)_  | exceção |
