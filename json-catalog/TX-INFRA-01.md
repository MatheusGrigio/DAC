## TX-INFRA-01 — Health check

> Voltar ao [índice do catálogo](../00-JSON-CATALOG.md).


**Tutorial:** [transacoes/TX-INFRA-01-health.md](../transacoes/TX-INFRA-01-health.md)

### Request

```http
GET /health HTTP/1.1
Host: localhost:3000
```

### Response 200 — Gateway no ar

> Sem `_links` (exceção HATEOAS).

```json
{
  "status": "UP"
}
```

---
