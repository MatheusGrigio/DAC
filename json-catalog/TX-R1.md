## TX-R1 — Autocadastro

> Voltar ao [índice do catálogo](../00-JSON-CATALOG.md).


**Tutorial:** [transacoes/TX-R1-autocadastro.md](../transacoes/TX-R1-autocadastro.md)

> Público · cria solicitação `PENDENTE` · **não** cria conta, Auth nem senha.

### Request

```http
POST /solicitacoes HTTP/1.1
Host: localhost:3000
Content-Type: application/json
```

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
  }
}
```

> `salario` é `string` `"4500.00"` — nunca `number` `4500`.

### Response 201 — Solicitação Beltrano (aceite T09)

Header: `Location: /solicitacoes/22233344405`

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
  "status": "PENDENTE",
  "motivo": null,
  "dataHoraProcessamento": null,
  "_links": {
    "self":      { "href": "http://localhost:3000/solicitacoes/22233344405" },
    "aprovacao": { "href": "http://localhost:3000/solicitacoes/22233344405/aprovacao" },
    "rejeicao":  { "href": "http://localhost:3000/solicitacoes/22233344405/rejeicao" }
  }
}
```

### Response 201 — Solicitação registrada

Header: `Location: /solicitacoes/11122233396`

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

> `aprovacao` e `rejeicao` **só existem enquanto `PENDENTE`**. Após rejeição ou aprovação esses rels desaparecem.

### Erros

**409 — CPF já possui solicitação (qualquer estado):**

```json
{
  "status": 409,
  "erro": "Conflict",
  "mensagem": "CPF já possui solicitação"
}
```

**409 — E-mail já usado em outra solicitação:**

```json
{
  "status": 409,
  "erro": "Conflict",
  "mensagem": "E-mail já usado em outra solicitação"
}
```

**409 — CPF já é cliente do seed:**

```json
{
  "status": 409,
  "erro": "Conflict",
  "mensagem": "CPF já possui cadastro de cliente"
}
```

**400 — Salário como number, CEP com pontos, UF inválida:**

```json
{
  "status": 400,
  "erro": "Bad Request",
  "mensagem": "Requisição malformada"
}
```

---
