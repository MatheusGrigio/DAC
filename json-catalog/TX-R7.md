## TX-R7 — Extrato

> Voltar ao [índice do catálogo](../00-JSON-CATALOG.md).


**Tutorial:** [transacoes/TX-R7-extrato.md](../transacoes/TX-R7-extrato.md)

> CLIENTE dono · sem `inicio`/`fim` = últimos 30 dias · max 365 dias · GERENTE **403**.

### Request

```http
GET /contas/1291/extrato?inicio=2020-01-01&fim=2020-01-31 HTTP/1.1
Host: localhost:3000
x-access-token: <tokenCliente>
```

### Response 200 — Janeiro/2020 seed da Catharyna (7 movimentações)

> O front (Luxon) itera dia a dia somando `saldoAbertura` com as movimentações para montar a linha do tempo.

```json
{
  "numeroConta": "1291",
  "dataInicio": "2020-01-01",
  "dataFim": "2020-01-31",
  "saldoAbertura": "0.00",
  "movimentacoes": [
    {
      "dataHora": "2020-01-01T10:00:00",
      "tipo": "DEPOSITO",
      "valor": "1000.00",
      "origem": null,
      "destino": null
    },
    {
      "dataHora": "2020-01-01T11:00:00",
      "tipo": "DEPOSITO",
      "valor": "900.00",
      "origem": null,
      "destino": null
    },
    {
      "dataHora": "2020-01-01T12:00:00",
      "tipo": "SAQUE",
      "valor": "550.00",
      "origem": null,
      "destino": null
    },
    {
      "dataHora": "2020-01-01T13:00:00",
      "tipo": "SAQUE",
      "valor": "350.00",
      "origem": null,
      "destino": null
    },
    {
      "dataHora": "2020-01-10T15:00:00",
      "tipo": "DEPOSITO",
      "valor": "2000.00",
      "origem": null,
      "destino": null
    },
    {
      "dataHora": "2020-01-15T08:00:00",
      "tipo": "SAQUE",
      "valor": "500.00",
      "origem": null,
      "destino": null
    },
    {
      "dataHora": "2020-01-20T12:00:00",
      "tipo": "TRANSFERENCIA",
      "valor": "1700.00",
      "origem": {
        "numeroConta": "1291",
        "cpf": "12912861012",
        "nome": "Catharyna"
      },
      "destino": {
        "numeroConta": "0950",
        "cpf": "09506382000",
        "nome": "Cleuddônio"
      }
    }
  ],
  "_links": {
    "self":  { "href": "http://localhost:3000/contas/1291/extrato" },
    "conta": { "href": "http://localhost:3000/contas/1291" }
  }
}
```

> `origem` e `destino` preenchidos **somente em TRANSFERENCIA**. Em DEPOSITO/SAQUE são `null`.

### Response 200 — Padrão (sem query, últimos 30 dias, em 2026)

> Sem movimentos recentes no seed → lista vazia, `saldoAbertura` = saldo atual.

```json
{
  "numeroConta": "1291",
  "dataInicio": "2026-07-25",
  "dataFim": "2026-08-24",
  "saldoAbertura": "800.00",
  "movimentacoes": [],
  "_links": {
    "self":  { "href": "http://localhost:3000/contas/1291/extrato" },
    "conta": { "href": "http://localhost:3000/contas/1291" }
  }
}
```

### Response 200 — Últimos 30 dias após T04–T06 (aceite)

> Depósito `10.00`, saque `10.00`, transferência `100.00` para `0950` · `saldoAbertura` = saldo antes do primeiro movimento no intervalo.

```json
{
  "numeroConta": "1291",
  "dataInicio": "2026-07-25",
  "dataFim": "2026-08-24",
  "saldoAbertura": "700.00",
  "movimentacoes": [
    {
      "dataHora": "2026-08-18T16:50:00",
      "tipo": "DEPOSITO",
      "valor": "10.00",
      "origem": null,
      "destino": null
    },
    {
      "dataHora": "2026-08-18T16:51:00",
      "tipo": "SAQUE",
      "valor": "10.00",
      "origem": null,
      "destino": null
    },
    {
      "dataHora": "2026-08-18T16:52:00",
      "tipo": "TRANSFERENCIA",
      "valor": "100.00",
      "origem": {
        "numeroConta": "1291",
        "cpf": "12912861012",
        "nome": "Catharyna"
      },
      "destino": {
        "numeroConta": "0950",
        "cpf": "09506382000",
        "nome": "Cleuddônio"
      }
    }
  ],
  "_links": {
    "self":  { "href": "http://localhost:3000/contas/1291/extrato" },
    "conta": { "href": "http://localhost:3000/contas/1291" }
  }
}
```

### Erros

**422 — Intervalo maior que 365 dias:**

```json
{
  "status": 422,
  "erro": "Unprocessable Entity",
  "mensagem": "Intervalo maior que 365 dias"
}
```

**422 — `fim` anterior a `inicio`:**

```json
{
  "status": 422,
  "erro": "Unprocessable Entity",
  "mensagem": "Intervalo inválido: fim anterior ao início"
}
```

**403 — GERENTE (extrato é só CLIENTE dono).  404 — Conta inexistente.**

---
