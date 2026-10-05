# TX-R4 — Depósito

**ID:** `TX-R4`  
**Requisito:** R4  
**Tipo:** command síncrono + Event Sourcing; projeção CQRS **eventual**  
**Diagrama de sequência:** CLIENTE dono → Gateway `POST /contas/{numero}/deposito` → MS Conta command (posse via `X-User-CPF`) → append evento `Depósito` → publica `ms.conta.events` → query atualiza saldo → **201 sem saldo novo**  
**Pré-requisito:** reboot + login `cli1`. Conta `1291` saldo `"800.00"`.

Não é SAGA. Não deposite na conta de outro cliente (403). Valor sempre string `"10.00"`.

---

## Passo a passo no HTTPie Desktop

1. Método **POST**.
2. URL: `{{baseUrl}}/contas/1291/deposito`
3. Headers: `x-access-token: {{tokenCliente}}`
4. Body → JSON:

```json
{
  "valor": "10.00"
}
```

5. **Send**.

---

## Resposta esperada

**HTTP 201** — **não** existe campo `saldo`. `dataHora` é o instante em `America/Sao_Paulo`. `destino` é nulo (não é transferência).

```json
{
  "numeroConta": "1291",
  "tipo": "DEPOSITO",
  "dataHora": "2026-08-18T16:50:00",
  "valor": "10.00",
  "destino": null,
  "_links": {
    "conta": { "href": "http://localhost:3000/contas/1291" },
    "extrato": { "href": "http://localhost:3000/contas/1291/extrato" }
  }
}
```

---

## Conferir o saldo (consistência eventual)

`GET {{baseUrl}}/contas/1291` com o mesmo token. Esperado após a projeção:

```json
{
  "numero": "1291",
  "saldo": "810.00"
}
```

Se ainda for `"800.00"`, espere 2 s e **Send** de novo (até 3 vezes / 5 s, como os testes de contrato).

---

## Erros

**HTTP 403** — `cli1` em `/contas/0950/deposito`.  
**HTTP 400** — `"valor": 10` (number) ou `"10.0"` (não tem 2 casas).  
**HTTP 404** — conta inexistente.

Para voltar ao seed: [TX-INFRA-02](./TX-INFRA-02-reboot.md).
