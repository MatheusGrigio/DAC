## TX-R8A — Listar solicitações

> Voltar ao [índice do catálogo](../00-JSON-CATALOG.md).


**Tutorial:** [transacoes/TX-R8A-listar-solicitacoes.md](../transacoes/TX-R8A-listar-solicitacoes.md)

> GERENTE · filtro opcional `?status=PENDENTE|APROVADA|NAO_APROVADA`.

### Request

```http
GET /solicitacoes HTTP/1.1
Host: localhost:3000
x-access-token: <tokenGerente>
```

_(ou `GET /solicitacoes?status=PENDENTE`)_

### Response 200 — Lista com item PENDENTE

> `_links.self` da lista inclui query string se filtrado.

```json
{
  "solicitacoes": [
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
  ],
  "_links": {
    "self": { "href": "http://localhost:3000/solicitacoes" }
  }
}
```

> Solicitação já processada (APROVADA ou NAO_APROVADA) **não** traz `aprovacao` nem `rejeicao` — apenas `self`.

### Com filtro `?status=PENDENTE`

```json
{
  "solicitacoes": [ ... ],
  "_links": {
    "self": { "href": "http://localhost:3000/solicitacoes?status=PENDENTE" }
  }
}
```

### Erros

**403 — token de CLIENTE:**

```json
{
  "status": 403,
  "erro": "Forbidden",
  "mensagem": "Acesso negado"
}
```

**401 — sem token:** `{ "auth": false, "message": "Token não fornecido." }`

---
