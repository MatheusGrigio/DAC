## TX-R9 — Aprovar cliente (SAGA)

> Voltar ao [índice do catálogo](../00-JSON-CATALOG.md).


**Tutorial:** [transacoes/TX-R9-aprovar-cliente.md](../transacoes/TX-R9-aprovar-cliente.md)

> GERENTE · sem body · Gateway **não** pré-valida PENDENTE (falha vai para o job).

### Request

```http
POST /solicitacoes/22233344405/aprovacao HTTP/1.1
Host: localhost:3000
x-access-token: <tokenGerente>
```

_(sem body)_

### Response 202 — Job aceito

Header: `Location: /jobs/8f14e45f-ceea-4e1b-9d2a-52f7254b1f0b/status`

> Sem `_links`. Sem `senha`. Não há 404/409 aqui — falha vai para o job.

```json
{
  "jobId": "8f14e45f-ceea-4e1b-9d2a-52f7254b1f0b",
  "status": "PENDENTE"
}
```

### Polling → GET /jobs/{jobId}/status → CONCLUIDO (ver [TX-JOB-01](TX-JOB-01.md))

```json
{
  "jobId": "8f14e45f-ceea-4e1b-9d2a-52f7254b1f0b",
  "status": "CONCLUIDO",
  "resultType": "resource",
  "dominio": "clientes",
  "resourceId": "22233344405"
}
```

> `resultType = "resource"` → ir para `GET /clientes/22233344405`. **Não** usar [TX-JOB-02](TX-JOB-02.md).

### Job FALHA (CPF inexistente ou já aprovado/rejeitado)

```json
{
  "jobId": "8f14e45f-ceea-4e1b-9d2a-52f7254b1f0b",
  "status": "FALHA",
  "erro": "Solicitação não encontrada ou já processada"
}
```

### Job FALHA (e-mail já de gerente — caso especial)

```json
{
  "jobId": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
  "status": "FALHA",
  "erro": "E-mail já cadastrado"
}
```

> Neste caso a solicitação fica `NAO_APROVADA` (com motivo automático), não volta para PENDENTE.

---
