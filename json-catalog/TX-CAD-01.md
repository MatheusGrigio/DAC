## TX-CAD-01 — Consultar cliente

> Voltar ao [índice do catálogo](../00-JSON-CATALOG.md).


**Tutorial:** [transacoes/TX-CAD-01-consultar-cliente.md](../transacoes/TX-CAD-01-consultar-cliente.md)

> GERENTE ou próprio CLIENTE · cacheado 5 min · sem `saldo`.

### Request

```http
GET /clientes/12912861012 HTTP/1.1
Host: localhost:3000
x-access-token: <tokenGerente ou tokenCliente próprio>
```

### Response 200 — Cliente (seed Catharyna)

```json
{
  "cpf": "12912861012",
  "nome": "Catharyna",
  "email": "cli1@bantads.com.br",
  "telefone": "41999990001",
  "salario": "10000.00",
  "endereco": {
    "logradouro": "Rua XV de Novembro",
    "numero": "1299",
    "complemento": null,
    "cep": "80060000",
    "cidade": "Curitiba",
    "uf": "PR"
  },
  "_links": {
    "self":  { "href": "http://localhost:3000/clientes/12912861012" },
    "conta": { "href": "http://localhost:3000/clientes/12912861012/conta" }
  }
}
```

> Não há `saldo` neste DTO. Para ver saldo: seguir `_links.conta` → [TX-R3A](#tx-r3a--conta-por-cpf).

### Response 200 — Cliente Beltrano (após TX-R9)

```json
{
  "cpf": "22233344405",
  "nome": "Beltrano de Tal",
  "email": "beltrano@exemplo.com.br",
  "telefone": "41999990000",
  "salario": "4500.00",
  "endereco": {
    "logradouro": "Rua XV de Novembro",
    "numero": "1299",
    "complemento": null,
    "cep": "80060000",
    "cidade": "Curitiba",
    "uf": "PR"
  },
  "_links": {
    "self":  { "href": "http://localhost:3000/clientes/22233344405" },
    "conta": { "href": "http://localhost:3000/clientes/22233344405/conta" }
  }
}
```

### Erros

**403 — CLIENTE consultando outro CPF:**

```json
{
  "status": 403,
  "erro": "Forbidden",
  "mensagem": "Acesso negado"
}
```

**404 — CPF não é cliente (pode ser solicitação PENDENTE ainda):**

```json
{
  "status": 404,
  "erro": "Not Found",
  "mensagem": "Cliente não encontrado"
}
```

---
