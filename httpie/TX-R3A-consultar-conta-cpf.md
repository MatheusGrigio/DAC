# TX-R3A — Tela inicial do cliente: conta por CPF (R3)

**ID:** `TX-R3A`  
**Requisito:** R3  
**Tipo:** consulta CQRS (lado **query**; não cachear)  
**Diagrama de sequência:** CLIENTE (ou GERENTE) → Gateway `GET /clientes/{cpf}/conta` → MS Conta query (read model) → `Conta` + `_links` de operações  
**Pré-requisito:** reboot + [TX-R2A](./TX-R2A-login.md) `cli1` → `tokenCliente`.

Ponto de partida da UI do cliente: número da conta, saldo e menu via HATEOAS. Cliente **não** consulta conta de outro CPF (403). Gerente vê a conta **sem** links de depósito/saque/transferência/extrato.

---

## Passo a passo no HTTPie Desktop

1. Método **GET**.
2. URL: `{{baseUrl}}/clientes/12912861012/conta`
3. Headers: `x-access-token: {{tokenCliente}}`
4. **Send**.

---

## Resposta esperada (cliente dono, seed)

**HTTP 200**

```json
{
  "numero": "1291",
  "cpfCliente": "12912861012",
  "cpfGerente": "98574307084",
  "saldo": "800.00",
  "dataCriacao": "2000-01-01",
  "_links": {
    "self": { "href": "http://localhost:3000/contas/1291" },
    "cliente": { "href": "http://localhost:3000/clientes/12912861012" },
    "deposito": { "href": "http://localhost:3000/contas/1291/deposito" },
    "saque": { "href": "http://localhost:3000/contas/1291/saque" },
    "transferencia": { "href": "http://localhost:3000/contas/1291/transferencia" },
    "extrato": { "href": "http://localhost:3000/contas/1291/extrato" }
  }
}
```

`self` canônico é `/contas/{numero}` ([TX-R3B](./TX-R3B-consultar-conta-numero.md)).

---

## Mesma URL com gerente

`x-access-token: {{tokenGerente}}` → **200**, mesmos dados de saldo, **sem** `deposito`/`saque`/`transferencia`/`extrato`.

---

## Erros

**HTTP 403** — Catharyna pede a conta de Cleuddônio (`/clientes/09506382000/conta`):

```json
{
  "status": 403,
  "erro": "Forbidden",
  "mensagem": "Acesso negado"
}
```

**HTTP 401** sem token.
