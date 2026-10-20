# TX-R15 — Remoção de gerente (SAGA)

**ID:** `TX-R15`  
**Requisito:** R15  
**Tipo:** assíncrono — SAGA; resultado **inline** (`GET /jobs/{id}/result`)  
**Diagrama de sequência:**  
1. Gateway: se CPF autenticado = CPF do path → **403 síncrono** (não inicia SAGA).  
2. Senão: **202** + `saga.cmd`.  
3. Orquestrador: MS Gerente inativa; MS Auth desativa; Redis logout forçado (`sessao:cpf` + `sessao:jti`); transfere contas ao ativo com **menos** clientes (≠ removido); e-mail FF a cada cliente.  
4. Job `CONCLUIDO` `resultType=inline`.  
**Pré-requisito:** reboot + login **Geniéve** (`ger1`). Remova **Gadamântio** (0 contas) — caso dos testes de contrato. Não remova a si mesmo.

Último gerente ativo: a SAGA recusa com job `FALHA` (`"Não é permitido remover o último gerente ativo"`), não com 4xx do DELETE.

---

## Passo a passo no HTTPie Desktop

### A) Pré-condição 403 — auto-remoção

`DELETE {{baseUrl}}/gerentes/98574307084`  
`x-access-token: {{tokenGerente}}` (Geniéve)

**HTTP 403** (não há job)

```json
{
  "status": 403,
  "erro": "Forbidden",
  "mensagem": "Não é permitido remover a si mesmo"
}
```

### B) Remover Gadamântio

1. Método **DELETE**.
2. URL: `{{baseUrl}}/gerentes/40501740066`
3. Headers: `x-access-token: {{tokenGerente}}`
4. Sem body. **Send**.

**HTTP 202**

```json
{
  "jobId": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
  "status": "PENDENTE"
}
```

`Location: /jobs/<jobId>/status`. Cole `jobId`.

### C) Polling — [TX-JOB-01](./TX-JOB-01-status.md)

```json
{
  "jobId": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
  "status": "CONCLUIDO",
  "resultType": "inline"
}
```

### D) Resultado inline — [TX-JOB-02](./TX-JOB-02-result.md)

`GET {{baseUrl}}/jobs/{{jobId}}/result`

**HTTP 200** — sem `_links`. Gadamântio não tinha contas; o destino da regra (ativo com menos clientes) no seed é Gyândula:

```json
{
  "mensagem": "Gerente removido; 0 contas transferidas para Gyândula"
}
```

### E) Login do removido

`POST {{baseUrl}}/login`

```json
{
  "email": "ger4@bantads.com.br",
  "senha": "tads"
}
```

**HTTP 401** `{ "auth": false, "message": "Login inválido!" }` (Auth inativo).

---

## Job FALHA (DELETE ainda é 202)

- Remover de novo o mesmo CPF (já inativo).
- CPF inexistente `00000000000`.

```json
{
  "jobId": "…",
  "status": "FALHA",
  "erro": "…"
}
```

`GET /jobs/{id}/result` nesses casos → **409** (`Job ainda não concluído, falhou ou não é inline`).
