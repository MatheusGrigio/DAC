## TX-R3B — Conta por número

> Voltar ao [índice do catálogo](../00-JSON-CATALOG.md).


**Tutorial:** [transacoes/TX-R3B-consultar-conta-numero.md](../transacoes/TX-R3B-consultar-conta-numero.md)

> GERENTE ou CLIENTE dono · `self` canônico da conta · número é string de 4 dígitos.

### Request

```http
GET /contas/1291 HTTP/1.1
Host: localhost:3000
x-access-token: <tokenCliente>
```

> Zero à esquerda preservado: `GET /contas/0950` (quatro caracteres, não `950`).

### Response 200 — CLIENTE dono

> Mesma estrutura de [TX-R3A](#tx-r3a--conta-por-cpf).

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

### Erro 404 — Número inexistente

```json
{
  "status": 404,
  "erro": "Not Found",
  "mensagem": "Conta não encontrada"
}
```

### Erro 403 — CLIENTE de outra conta

```json
{
  "status": 403,
  "erro": "Forbidden",
  "mensagem": "Acesso negado"
}
```

---
