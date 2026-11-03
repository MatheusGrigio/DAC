## TX-R8B — Consultar solicitação

> Voltar ao [índice do catálogo](../00-JSON-CATALOG.md).


**Tutorial:** [transacoes/TX-R8B-consultar-solicitacao.md](../transacoes/TX-R8B-consultar-solicitacao.md)

### Request

```http
GET /solicitacoes/11122233396 HTTP/1.1
Host: localhost:3000
x-access-token: <tokenGerente>
```

### Response 200 — Solicitação PENDENTE

```json
{
  "cpf": "11122233396",
  "nome": "Fulano de Tal",
  "email": "fulano@exemplo.com.br",
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
  "status": "PENDENTE",
  "motivo": null,
  "dataHoraProcessamento": null,
  "_links": {
    "self":      { "href": "http://localhost:3000/solicitacoes/11122233396" },
    "aprovacao": { "href": "http://localhost:3000/solicitacoes/11122233396/aprovacao" },
    "rejeicao":  { "href": "http://localhost:3000/solicitacoes/11122233396/rejeicao" }
  }
}
```

### Response 200 — Solicitação NAO_APROVADA (após TX-R10)

> `aprovacao` e `rejeicao` ausentes. `motivo` e `dataHoraProcessamento` preenchidos.

```json
{
  "cpf": "11122233396",
  "nome": "Fulano de Tal",
  "email": "fulano@exemplo.com.br",
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
  "status": "NAO_APROVADA",
  "motivo": "Renda incompatível com a política do banco",
  "dataHoraProcessamento": "2026-08-18T16:40:00",
  "_links": {
    "self": { "href": "http://localhost:3000/solicitacoes/11122233396" }
  }
}
```

### Response 200 — NAO_APROVADA (falha R9 — e-mail de gerente)

```json
{
  "cpf": "33344455516",
  "nome": "Email de Gerente",
  "email": "ger1@bantads.com.br",
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
  "status": "NAO_APROVADA",
  "motivo": "E-mail já cadastrado",
  "dataHoraProcessamento": "2026-08-18T16:45:00",
  "_links": {
    "self": { "href": "http://localhost:3000/solicitacoes/33344455516" }
  }
}
```

### Erros

**404 — CPF sem solicitação:**

```json
{
  "status": 404,
  "erro": "Not Found",
  "mensagem": "Solicitação não encontrada"
}
```

**403 — CLIENTE autenticado:**

```json
{
  "status": 403,
  "erro": "Forbidden",
  "mensagem": "Acesso negado"
}
```

---
