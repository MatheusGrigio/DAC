## TX-R5 — Saque

> Voltar ao [índice do catálogo](../00-JSON-CATALOG.md).


**Tutorial:** [transacoes/TX-R5-saque.md](../transacoes/TX-R5-saque.md)

> CLIENTE dono · saldo validado no command por replay (nunca no read model).

### Request

```http
POST /contas/1291/saque HTTP/1.1
Host: localhost:3000
Content-Type: application/json
x-access-token: <tokenCliente>
```

```json
{
  "valor": "10.00"
}
```

### Response 201 — Saque registrado

```json
{
  "numeroConta": "1291",
  "tipo": "SAQUE",
  "dataHora": "2026-08-18T16:51:00",
  "valor": "10.00",
  "destino": null,
  "_links": {
    "conta":   { "href": "http://localhost:3000/contas/1291" },
    "extrato": { "href": "http://localhost:3000/contas/1291/extrato" }
  }
}
```

> Após 2–5 s: `GET /contas/1291` → `"saldo": "790.00"` (seed puro `800 − 10`).

### Erros

**422 — Saldo insuficiente (validado no command por replay):**

```json
{
  "status": 422,
  "erro": "Unprocessable Entity",
  "mensagem": "Saldo insuficiente para a operação"
}
```

> Nenhum evento gravado. Saldo permanece inalterado.

**403 — Conta alheia ou GERENTE.  400 — Formato do valor.**

---
