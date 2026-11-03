## TX-R3A — Conta por CPF

> Voltar ao [índice do catálogo](../00-JSON-CATALOG.md).


**Tutorial:** [transacoes/TX-R3A-consultar-conta-cpf.md](../transacoes/TX-R3A-consultar-conta-cpf.md)

> GERENTE ou próprio CLIENTE · CQRS query · **sem cache** (saldo muda).

### Request

```http
GET /clientes/12912861012/conta HTTP/1.1
Host: localhost:3000
x-access-token: <tokenCliente>
```

### Response 200 — CLIENTE dono (rels de escrita presentes)

```json
{
  "numero": "1291",
  "cpfCliente": "12912861012",
  "cpfGerente": "98574307084",
  "saldo": "800.00",
  "dataCriacao": "2000-01-01",
  "_links": {
    "self":          { "href": "http://localhost:3000/contas/1291" },
    "cliente":       { "href": "http://localhost:3000/clientes/12912861012" },
    "deposito":      { "href": "http://localhost:3000/contas/1291/deposito" },
    "saque":         { "href": "http://localhost:3000/contas/1291/saque" },
    "transferencia": { "href": "http://localhost:3000/contas/1291/transferencia" },
    "extrato":       { "href": "http://localhost:3000/contas/1291/extrato" }
  }
}
```

### Response 200 — GERENTE (sem rels de escrita)

> `deposito`, `saque`, `transferencia`, `extrato` **ausentes** — Gateway remove conforme perfil.

```json
{
  "numero": "1291",
  "cpfCliente": "12912861012",
  "cpfGerente": "98574307084",
  "saldo": "800.00",
  "dataCriacao": "2000-01-01",
  "_links": {
    "self":    { "href": "http://localhost:3000/contas/1291" },
    "cliente": { "href": "http://localhost:3000/clientes/12912861012" }
  }
}
```

### Erros

**403 — CLIENTE tentando conta de outro:**

```json
{
  "status": 403,
  "erro": "Forbidden",
  "mensagem": "Acesso negado"
}
```

---
