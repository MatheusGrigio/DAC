# TX-R6 — Transferência

**ID:** `TX-R6`  
**Requisito:** R6  
**Tipo:** **não é SAGA** — um único MS Conta grava `TransferênciaOrigem` + `TransferênciaDestino` na mesma transação. O Gateway **enriquece** nomes/CPF antes.  
**Diagrama de sequência:** CLIENTE origem → Gateway `POST /contas/{origem}/transferencia` `{ contaDestino, valor }` → Gateway consulta query (CPF destino) + MS Cliente (nomes) → MS Conta command (replay saldo, atomicidade) → eventos + `ms.conta.events` → **201** com `destino` preenchido, **sem saldo**  
**Pré-requisito:** reboot (`1291` = `"800.00"`, `0950` = `"10000.00"`) + `tokenCliente` da Catharyna.

---

## Passo a passo no HTTPie Desktop

1. Método **POST**.
2. URL: `{{baseUrl}}/contas/1291/transferencia`
3. Headers: `x-access-token: {{tokenCliente}}`
4. Body → JSON (o front **não** envia nomes; só conta destino e valor):

```json
{
  "contaDestino": "0950",
  "valor": "100.00"
}
```

5. **Send**.

---

## Resposta esperada

**HTTP 201**

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
    "conta": { "href": "http://localhost:3000/contas/1291" },
    "extrato": { "href": "http://localhost:3000/contas/1291/extrato" }
  }
}
```

### Saldos após a projeção

| Request | Token | Saldo |
|---|---|---|
| `GET /contas/1291` | Catharyna | `"700.00"` |
| `GET /contas/0950` | `cli2@bantads.com.br` | `"10100.00"` |

Espere 2–5 s se a query ainda mostrar o valor antigo.

---

## Casos 422 (regra de negócio síncrona)

Mesma conta:

```json
{ "contaDestino": "1291", "valor": "100.00" }
```

```json
{
  "status": 422,
  "erro": "Unprocessable Entity",
  "mensagem": "Não é permitido transferir para a própria conta"
}
```

Destino inexistente:

```json
{ "contaDestino": "0001", "valor": "100.00" }
```

```json
{
  "status": 422,
  "erro": "Unprocessable Entity",
  "mensagem": "Conta destino inexistente"
}
```

Valor maior que o saldo: mesma envelope 422, `"Saldo insuficiente para a operação"`.

**HTTP 403** — origem de outra pessoa. **HTTP 400** — `contaDestino` com 3 dígitos ou valor number.
