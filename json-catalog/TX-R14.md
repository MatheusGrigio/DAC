## TX-R14 — Atualizar gerente

> Voltar ao [índice do catálogo](../00-JSON-CATALOG.md).


**Tutorial:** [transacoes/TX-R14-atualizar-gerente.md](../transacoes/TX-R14-atualizar-gerente.md)

> GERENTE · só `nome` e `telefone` mutáveis · invalida cache.

### Request

```http
PUT /gerentes/98574307084 HTTP/1.1
Host: localhost:3000
Content-Type: application/json
x-access-token: <tokenGerente>
```

```json
{
  "nome": "Geniéve Silva",
  "telefone": "41988889999"
}
```

### Response 200 — Atualizado

```json
{
  "cpf": "98574307084",
  "nome": "Geniéve Silva",
  "email": "ger1@bantads.com.br",
  "telefone": "41988889999",
  "ativo": true,
  "quantidadeClientes": null,
  "_links": {
    "self":        { "href": "http://localhost:3000/gerentes/98574307084" },
    "atualizacao": { "href": "http://localhost:3000/gerentes/98574307084" }
  }
}
```

> Cache invalidado → próximo GET retorna o novo nome imediatamente.

### Erros

**400 — Tentativa de mudar e-mail ou CPF:**

```json
{
  "status": 400,
  "erro": "Bad Request",
  "mensagem": "CPF e e-mail são imutáveis"
}
```

**409 — Gerente inativo:**

```json
{
  "status": 409,
  "erro": "Conflict",
  "mensagem": "Gerente inativo"
}
```

**404 — CPF inexistente:**

```json
{
  "status": 404,
  "erro": "Not Found",
  "mensagem": "Gerente não encontrado"
}
```

**403 — CLIENTE.**

---
