## TX-R10 — Rejeitar cliente

> Voltar ao [índice do catálogo](../00-JSON-CATALOG.md).


**Tutorial:** [transacoes/TX-R10-rejeitar-cliente.md](../transacoes/TX-R10-rejeitar-cliente.md)

> GERENTE · síncrono · 200 (não 202) · e-mail fire-and-forget.

### Request

```http
POST /solicitacoes/11122233396/rejeicao HTTP/1.1
Host: localhost:3000
Content-Type: application/json
x-access-token: <tokenGerente>
```

```json
{
  "motivo": "Renda incompatível com a política do banco"
}
```

### Response 200 — Rejeitada

> `aprovacao` e `rejeicao` **saem** dos `_links`. `dataHoraProcessamento` gerado no servidor (fuso São Paulo).

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

### Erros

**409 — Já processada (rejeitar de novo):**

```json
{
  "status": 409,
  "erro": "Conflict",
  "mensagem": "Solicitação não está PENDENTE"
}
```

**400 — Sem `motivo` ou motivo vazio:**

```json
{
  "status": 400,
  "erro": "Bad Request",
  "mensagem": "Requisição malformada"
}
```

**404 — CPF sem solicitação.**
**403 — CLIENTE.**

---
