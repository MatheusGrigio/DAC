## TX-JOB-01 — Status do job

> Voltar ao [índice do catálogo](../00-JSON-CATALOG.md).


**Tutorial:** [transacoes/TX-JOB-01-status.md](../transacoes/TX-JOB-01-status.md)

> Dono do job (mesmo CPF do 202) · sem `_links` · TTL 5 min.

### Request

```http
GET /jobs/8f14e45f-ceea-4e1b-9d2a-52f7254b1f0b/status HTTP/1.1
Host: localhost:3000
x-access-token: <mesmo token do POST/DELETE que criou o job>
```

### Response 200 — PENDENTE (ainda rodando)

```json
{
  "jobId": "8f14e45f-ceea-4e1b-9d2a-52f7254b1f0b",
  "status": "PENDENTE"
}
```

### Response 200 — CONCLUIDO resource (R9 / R13)

```json
{
  "jobId": "8f14e45f-ceea-4e1b-9d2a-52f7254b1f0b",
  "status": "CONCLUIDO",
  "resultType": "resource",
  "dominio": "clientes",
  "resourceId": "22233344405"
}
```

> Para R13: `"dominio": "gerentes"`.  
> Próximo passo: `GET /{dominio}/{resourceId}`. **Não** chamar `/result`.

### Response 200 — CONCLUIDO inline (R15 / R16)

```json
{
  "jobId": "8f14e45f-ceea-4e1b-9d2a-52f7254b1f0b",
  "status": "CONCLUIDO",
  "resultType": "inline"
}
```

> Próximo passo: [TX-JOB-02](TX-JOB-02.md).

### Response 200 — FALHA

```json
{
  "jobId": "8f14e45f-ceea-4e1b-9d2a-52f7254b1f0b",
  "status": "FALHA",
  "erro": "E-mail já cadastrado"
}
```

> `erro` é mensagem de negócio (string), **não** o envelope `{ status, erro, mensagem }`.

### Erros

**404 — UUID inexistente ou TTL expirado (> 5 min):**

```json
{
  "status": 404,
  "erro": "Not Found",
  "mensagem": "Job inexistente ou expirado"
}
```

**403 — Outro usuário tentando ver o job:**

```json
{
  "status": 403,
  "erro": "Forbidden",
  "mensagem": "Job não pertence ao usuário autenticado"
}
```

---
