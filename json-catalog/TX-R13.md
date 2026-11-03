## TX-R13 — Inserir gerente (SAGA)

> Voltar ao [índice do catálogo](../00-JSON-CATALOG.md).


**Tutorial:** [transacoes/TX-R13-inserir-gerente.md](../transacoes/TX-R13-inserir-gerente.md)

> GERENTE · senha no formulário (não vai por e-mail) · unicidade de e-mail verificada **dentro da SAGA**.

### Request

```http
POST /gerentes HTTP/1.1
Host: localhost:3000
Content-Type: application/json
x-access-token: <tokenGerente>
```

```json
{
  "cpf": "55667788990",
  "nome": "Gumercindo",
  "email": "ger5@bantads.com.br",
  "telefone": "41988880005",
  "senha": "tads"
}
```

> `senha` obrigatória · nunca volta no response · nunca vai para Redis da SAGA.

### Response 202 — Job aceito

Header: `Location: /jobs/11111111-2222-3333-4444-555555555555/status`

```json
{
  "jobId": "11111111-2222-3333-4444-555555555555",
  "status": "PENDENTE"
}
```

### Polling → CONCLUIDO resource

```json
{
  "jobId": "11111111-2222-3333-4444-555555555555",
  "status": "CONCLUIDO",
  "resultType": "resource",
  "dominio": "gerentes",
  "resourceId": "55667788990"
}
```

> `resultType = "resource"` → `GET /gerentes/55667788990`.

### GET /gerentes/55667788990 — Recurso criado

```json
{
  "cpf": "55667788990",
  "nome": "Gumercindo",
  "email": "ger5@bantads.com.br",
  "telefone": "41988880005",
  "ativo": true,
  "quantidadeClientes": null,
  "_links": {
    "self":        { "href": "http://localhost:3000/gerentes/55667788990" },
    "atualizacao": { "href": "http://localhost:3000/gerentes/55667788990" },
    "remocao":     { "href": "http://localhost:3000/gerentes/55667788990" }
  }
}
```

### GET — Gumercindo autenticado (sem `remocao`)

```json
{
  "cpf": "55667788990",
  "nome": "Gumercindo",
  "email": "ger5@bantads.com.br",
  "telefone": "41988880005",
  "ativo": true,
  "quantidadeClientes": null,
  "_links": {
    "self":        { "href": "http://localhost:3000/gerentes/55667788990" },
    "atualizacao": { "href": "http://localhost:3000/gerentes/55667788990" }
  }
}
```

### Job FALHA — E-mail duplicado

```json
{
  "jobId": "11111111-2222-3333-4444-555555555555",
  "status": "FALHA",
  "erro": "E-mail já cadastrado"
}
```

> Compensação: `GET /gerentes/{cpfNovo}` → **404** (não ficou órfão).

### Erro síncrono 400 — Body incompleto (sem `senha`, `cpf`, etc.)

```json
{
  "status": 400,
  "erro": "Bad Request",
  "mensagem": "Requisição malformada"
}
```

> Sem `jobId`. 400 síncrono antes de criar job.

---
