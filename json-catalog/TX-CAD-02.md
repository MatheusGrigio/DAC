## TX-CAD-02 — Consultar gerente

> Voltar ao [índice do catálogo](../00-JSON-CATALOG.md).


**Tutorial:** [transacoes/TX-CAD-02-consultar-gerente.md](../transacoes/TX-CAD-02-consultar-gerente.md)

> GERENTE · cacheado 5 min · `quantidadeClientes` pode ser `null` (contagem vive na listagem R12).

### Request

```http
GET /gerentes/98574307084 HTTP/1.1
Host: localhost:3000
x-access-token: <tokenGerente>   (Geniéve)
```

### Response 200 — Próprio gerente autenticado (sem `remocao`)

```json
{
  "cpf": "98574307084",
  "nome": "Geniéve",
  "email": "ger1@bantads.com.br",
  "telefone": "41988880001",
  "ativo": true,
  "quantidadeClientes": null,
  "_links": {
    "self":        { "href": "http://localhost:3000/gerentes/98574307084" },
    "atualizacao": { "href": "http://localhost:3000/gerentes/98574307084" }
  }
}
```

### Response 200 — Outro gerente (com `remocao`)

`GET /gerentes/40501740066` (Gadamântio)

```json
{
  "cpf": "40501740066",
  "nome": "Gadamântio",
  "email": "ger4@bantads.com.br",
  "telefone": "41988880004",
  "ativo": true,
  "_links": {
    "self":        { "href": "http://localhost:3000/gerentes/40501740066" },
    "atualizacao": { "href": "http://localhost:3000/gerentes/40501740066" },
    "remocao":     { "href": "http://localhost:3000/gerentes/40501740066" }
  }
}
```

> `quantidadeClientes` pode ser `null` neste recurso unitário — não é erro.

### Erros

**404 — CPF inexistente:**

```json
{
  "status": 404,
  "erro": "Not Found",
  "mensagem": "Gerente não encontrado"
}
```

---
