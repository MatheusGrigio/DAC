# TX-R7 — Extrato

**ID:** `TX-R7`  
**Requisito:** R7  
**Tipo:** consulta CQRS query; o **front** monta o saldo dia a dia (Luxon) a partir de `saldoAbertura` + `movimentacoes`  
**Diagrama de sequência:** CLIENTE dono → Gateway `GET /contas/{numero}/extrato?inicio=&fim=` → MS Conta query (histórico; intervalo máx. 365 dias)  
**Pré-requisito:** `tokenCliente` da Catharyna. Gerente **não** acessa extrato (403).

Sem `inicio`/`fim`: últimos **30 dias**. Query `YYYY-MM-DD`.

---

## Passo a passo no HTTPie Desktop

### A) Janeiro/2020 (seed da Catharyna — 7 movimentações)

1. Método **GET**.
2. URL:

```
{{baseUrl}}/contas/1291/extrato?inicio=2020-01-01&fim=2020-01-31
```

3. Headers: `x-access-token: {{tokenCliente}}`
4. **Send**.

**HTTP 200** — `saldoAbertura` `"0.00"` (só o evento `Criado` em 2000). Sete linhas; a última é a transferência para Cleuddônio.

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
    "self": { "href": "http://localhost:3000/contas/1291/extrato" },
    "conta": { "href": "http://localhost:3000/contas/1291" }
  }
}
```

(O `href` de `self` pode incluir a query string usada.) Entrada vs saída na UI: comparar o CPF logado com `origem`/`destino`; saque/transferência de saída em vermelho, depósito/entrada em azul.

### B) Padrão (últimos 30 dias)

```
{{baseUrl}}/contas/1291/extrato
```

Com a data de hoje em 2026 e seed sem movimentos recentes: `movimentacoes` `[]` e `saldoAbertura` igual ao saldo atual (`"800.00"` se não houve R4/R5/R6).

---

## Erros 422

Intervalo > 365 dias:

```
{{baseUrl}}/contas/1291/extrato?inicio=2020-01-01&fim=2021-01-02
```

```json
{
  "status": 422,
  "erro": "Unprocessable Entity",
  "mensagem": "Intervalo maior que 365 dias"
}
```

`fim` anterior a `inicio`: `"Intervalo inválido: fim anterior ao início"`.

**HTTP 403** — gerente ou outro cliente. **HTTP 404** — conta inexistente.
