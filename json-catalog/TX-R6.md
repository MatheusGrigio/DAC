## TX-R6 — Transferência

> Voltar ao [índice do catálogo](../00-JSON-CATALOG.md).


**Tutorial:** [transacoes/TX-R6-transferencia.md](../transacoes/TX-R6-transferencia.md)

> CLIENTE dono da origem · **não é SAGA** · Gateway enriquece CPF e nomes antes de enviar ao MS Conta.

### Request

> O front envia **apenas** `contaDestino` e `valor`. O Gateway busca CPF e nomes internamente.

```http
POST /contas/1291/transferencia HTTP/1.1
Host: localhost:3000
Content-Type: application/json
x-access-token: <tokenCliente>
```

```json
{
  "contaDestino": "0950",
  "valor": "100.00"
}
```

### Response 201 — Transferência registrada

> `destino` preenchido pelo Gateway (enriquecido com CPF e nome). Sem `saldo`.

```json
{
  "numeroConta": "1291",
  "tipo": "TRANSFERENCIA",
  "dataHora": "2026-08-18T16:52:00",
  "valor": "100.00",
  "destino": {
    "numeroConta": "0950",
    "cpf": "09506382000",
    "nome": "Cleuddônio"
  },
  "_links": {
    "conta":   { "href": "http://localhost:3000/contas/1291" },
    "extrato": { "href": "http://localhost:3000/contas/1291/extrato" }
  }
}
```

> Após 2–5 s: origem `1291` → `"700.00"` · destino `0950` → `"10100.00"`.

### Erros

**422 — Transferência para a própria conta:**

```json
{
  "status": 422,
  "erro": "Unprocessable Entity",
  "mensagem": "Não é permitido transferir para a própria conta"
}
```

**422 — Conta destino inexistente:**

```json
{
  "status": 422,
  "erro": "Unprocessable Entity",
  "mensagem": "Conta destino inexistente"
}
```

**422 — Saldo insuficiente:**

```json
{
  "status": 422,
  "erro": "Unprocessable Entity",
  "mensagem": "Saldo insuficiente para a operação"
}
```

**403 — Conta de outro ou GERENTE.  400 — Formato inválido.**

---
