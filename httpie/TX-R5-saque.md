# TX-R5 — Saque

**ID:** `TX-R5`  
**Requisito:** R5  
**Tipo:** command síncrono + Event Sourcing; saldo validado no **command** (replay), nunca no read model  
**Diagrama de sequência:** CLIENTE dono → Gateway `POST /contas/{numero}/saque` → MS Conta replay → se saldo insuficiente **422** → senão evento `Saque` → `ms.conta.events` → **201 sem saldo**  
**Pré-requisito:** seed (saldo `"800.00"`) **ou** sequência R4 (saldo `"810.00"`). Os testes de contrato fazem depósito +10 e depois saque +10 voltando a 800. Abaixo assume **reboot puro**.

---

## Passo a passo no HTTPie Desktop

### A) Saldo insuficiente (obrigatório no diagrama)

`POST {{baseUrl}}/contas/1291/saque`  
`x-access-token: {{tokenCliente}}`

```json
{
  "valor": "900.00"
}
```

**HTTP 422**

```json
{
  "status": 422,
  "erro": "Unprocessable Entity",
  "mensagem": "Saldo insuficiente para a operação"
}
```

### B) Saque válido

```json
{
  "valor": "10.00"
}
```

**HTTP 201**

```json
{
  "numeroConta": "1291",
  "tipo": "SAQUE",
  "dataHora": "2026-08-18T16:51:00",
  "valor": "10.00",
  "destino": null,
  "_links": {
    "conta": { "href": "http://localhost:3000/contas/1291" },
    "extrato": { "href": "http://localhost:3000/contas/1291/extrato" }
  }
}
```

Reconsulte a conta: saldo projetado `"790.00"` (reboot puro) ou `"800.00"` se você tinha depositado `"10.00"` antes (fluxo T04+T05).

---

## Erros de posse

**HTTP 403** — sacar `/contas/0950` com token da Catharyna. Gerente também não saca (perfil CLIENTE dono).
