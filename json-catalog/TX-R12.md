## TX-R12 — Listar gerentes (Composition)

> Voltar ao [índice do catálogo](../00-JSON-CATALOG.md).


**Tutorial:** [transacoes/TX-R12-listar-gerentes.md](../transacoes/TX-R12-listar-gerentes.md)

> GERENTE · Composition MS Gerente + MS Conta · só ativos · ordenado pt-BR por nome.

### Request

```http
GET /gerentes HTTP/1.1
Host: localhost:3000
x-access-token: <tokenGerente>   (Geniéve — CPF 98574307084)
```

### Response 200 — Seed (Geniéve autenticada)

> Lista tem `_links.self` + `_links.criacao` (para POST R13).  
> Geniéve (autenticada): **sem** `remocao` em si mesma.  
> Demais: têm `remocao`.

```json
{
  "gerentes": [
    {
      "cpf": "40501740066",
      "nome": "Gadamântio",
      "email": "ger4@bantads.com.br",
      "telefone": "41988880004",
      "ativo": true,
      "quantidadeClientes": 0,
      "_links": {
        "self":       { "href": "http://localhost:3000/gerentes/40501740066" },
        "atualizacao":{ "href": "http://localhost:3000/gerentes/40501740066" },
        "remocao":    { "href": "http://localhost:3000/gerentes/40501740066" }
      }
    },
    {
      "cpf": "98574307084",
      "nome": "Geniéve",
      "email": "ger1@bantads.com.br",
      "telefone": "41988880001",
      "ativo": true,
      "quantidadeClientes": 2,
      "_links": {
        "self":        { "href": "http://localhost:3000/gerentes/98574307084" },
        "atualizacao": { "href": "http://localhost:3000/gerentes/98574307084" }
      }
    },
    {
      "cpf": "64065268052",
      "nome": "Godophredo",
      "email": "ger2@bantads.com.br",
      "telefone": "41988880002",
      "ativo": true,
      "quantidadeClientes": 2,
      "_links": {
        "self":        { "href": "http://localhost:3000/gerentes/64065268052" },
        "atualizacao": { "href": "http://localhost:3000/gerentes/64065268052" },
        "remocao":     { "href": "http://localhost:3000/gerentes/64065268052" }
      }
    },
    {
      "cpf": "23862179060",
      "nome": "Gyândula",
      "email": "ger3@bantads.com.br",
      "telefone": "41988880003",
      "ativo": true,
      "quantidadeClientes": 1,
      "_links": {
        "self":        { "href": "http://localhost:3000/gerentes/23862179060" },
        "atualizacao": { "href": "http://localhost:3000/gerentes/23862179060" },
        "remocao":     { "href": "http://localhost:3000/gerentes/23862179060" }
      }
    }
  ],
  "_links": {
    "self":    { "href": "http://localhost:3000/gerentes" },
    "criacao": { "href": "http://localhost:3000/gerentes" }
  }
}
```

---
