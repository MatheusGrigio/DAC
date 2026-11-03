## TX-R4 — Depósito

> Voltar ao [índice do catálogo](../00-JSON-CATALOG.md).


**Tutorial:** [transacoes/TX-R4-deposito.md](../transacoes/TX-R4-deposito.md)

> CLIENTE dono · event sourcing · **sem saldo no response** · consistência eventual (2–5 s).

### Request

```http
POST /contas/1291/deposito HTTP/1.1
Host: localhost:3000
Content-Type: application/json
x-access-token: <tokenCliente>
```

```json
{
  "valor": "10.00"
}
```

> `"valor"` é `string` `"10.00"` — nunca `number` `10`.

### Response 201 — Depósito registrado

> Sem `saldo`. Seguir `_links.conta` para reconsultar (CQRS eventual).

```json
{
  "numeroConta": "1291",
  "tipo": "DEPOSITO",
  "dataHora": "2026-08-18T16:50:00",
  "valor": "10.00",
  "destino": null,
  "_links": {
    "conta":   { "href": "http://localhost:3000/contas/1291" },
    "extrato": { "href": "http://localhost:3000/contas/1291/extrato" }
  }
}
```

> Após 2–5 s: `GET /contas/1291` → `"saldo": "810.00"` (seed `800 + 10`).

### Erro 400 — Valor como number ou formato incorreto

```json
{
  "status": 400,
  "erro": "Bad Request",
  "mensagem": "Requisição malformada"
}
```

### Erro 403 — GERENTE ou conta alheia (depósito)

```json
{
  "status": 403,
  "erro": "Forbidden",
  "mensagem": "Acesso negado"
}
```

---
