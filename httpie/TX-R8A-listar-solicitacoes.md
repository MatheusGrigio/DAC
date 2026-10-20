# TX-R8A — Listar solicitações de autocadastro (R8)

**ID:** `TX-R8A`  
**Requisito:** R8  
**Tipo:** consulta síncrona  
**Diagrama de sequência:** Gerente → Gateway `GET /solicitacoes` → MS Cliente → lista HAL (itens `PENDENTE` com links `aprovacao`/`rejeicao`)  
**Pré-requisito:** [TX-R2A](./TX-R2A-login.md) gerente + de preferência [TX-R1](./TX-R1-autocadastro.md) para haver item pendente.

Query opcional: `?status=PENDENTE` | `APROVADA` | `NAO_APROVADA`.

---

## Passo a passo no HTTPie Desktop

1. Método **GET**.
2. URL (todas):

```
{{baseUrl}}/solicitacoes
```

Ou só pendentes:

```
{{baseUrl}}/solicitacoes?status=PENDENTE
```

3. Headers:

| Header | Valor |
|---|---|
| `Accept` | `application/json` |
| `x-access-token` | `{{tokenGerente}}` |

4. **Send**.

---

## Resposta esperada

**HTTP 200** — após reboot + autocadastro de Fulano, o array contém pelo menos o item `11122233396` com `status: "PENDENTE"` e os dois botões HATEOAS. `_links.self` da lista aponta para a URL chamada (inclui `status=` se você filtrou).

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
        "self": { "href": "http://localhost:3000/solicitacoes/11122233396" },
        "aprovacao": { "href": "http://localhost:3000/solicitacoes/11122233396/aprovacao" },
        "rejeicao": { "href": "http://localhost:3000/solicitacoes/11122233396/rejeicao" }
      }
    }
  ],
  "_links": {
    "self": { "href": "http://localhost:3000/solicitacoes" }
  }
}
```

Solicitação já processada **não** traz `aprovacao` nem `rejeicao` — só `self`.

---

## Erros

**HTTP 401** sem token: `{ "auth": false, "message": "Token não fornecido." }`  
**HTTP 403** com `tokenCliente`:

```json
{
  "status": 403,
  "erro": "Forbidden",
  "mensagem": "Acesso negado"
}
```
