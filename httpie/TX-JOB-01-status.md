# TX-JOB-01 — Polling do status do job

**ID:** `TX-JOB-01`  
**Requisito:** seção 5.8 (jobs); usado por R9, R13, R15 e R16  
**Tipo:** consulta Redis no Gateway; **sem** `_links`  
**Diagrama de sequência:** mesmo usuário que iniciou a operação → Gateway `GET /jobs/{jobId}/status` → Redis `job:<id>` (TTL **5 min**) → `{ jobId, status, … }`  
**Pré-requisito:** um `jobId` de 202 (copie da resposta ou da header `Location`). Token **do mesmo CPF**.

`status`: `PENDENTE` | `CONCLUIDO` | `FALHA`. Em SAGA, `jobId` = `sagaId`.

---

## Passo a passo no HTTPie Desktop

1. Cole o UUID na variável `jobId`.
2. Método **GET**.
3. URL: `{{baseUrl}}/jobs/{{jobId}}/status`
4. Headers: `x-access-token` do **mesmo** usuário do 202 (`{{tokenGerente}}` nos fluxos R9/R13/R15/R16).
5. **Send**. Repita até sair de `PENDENTE` (R16 costuma ser instantâneo; SAGA até ~30 s por passo).

---

## Respostas esperadas

**HTTP 200 — ainda rodando**

```json
{
  "jobId": "8f14e45f-ceea-4e1b-9d2a-52f7254b1f0b",
  "status": "PENDENTE"
}
```

**HTTP 200 — SAGA R9/R13 ok** (`resultType=resource`)

```json
{
  "jobId": "8f14e45f-ceea-4e1b-9d2a-52f7254b1f0b",
  "status": "CONCLUIDO",
  "resultType": "resource",
  "dominio": "clientes",
  "resourceId": "22233344405"
}
```

(`dominio` = `gerentes` no R13.)

**HTTP 200 — R15/R16 ok** (`resultType=inline`) — em seguida [TX-JOB-02](./TX-JOB-02-result.md)

```json
{
  "jobId": "8f14e45f-ceea-4e1b-9d2a-52f7254b1f0b",
  "status": "CONCLUIDO",
  "resultType": "inline"
}
```

**HTTP 200 — falha de negócio da SAGA**

```json
{
  "jobId": "8f14e45f-ceea-4e1b-9d2a-52f7254b1f0b",
  "status": "FALHA",
  "erro": "E-mail já cadastrado"
}
```

---

## Erros

**HTTP 404** — UUID inexistente ou TTL estourado:

```json
{
  "status": 404,
  "erro": "Not Found",
  "mensagem": "Job inexistente ou expirado"
}
```

**HTTP 403** — outro usuário:

```json
{
  "status": 403,
  "erro": "Forbidden",
  "mensagem": "Job não pertence ao usuário autenticado"
}
```

**HTTP 401** — sem token.
