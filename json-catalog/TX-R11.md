## TX-R11 — Listar clientes (Composition)

> Voltar ao [índice do catálogo](../00-JSON-CATALOG.md).


**Tutorial:** [transacoes/TX-R11-consultar-clientes.md](../transacoes/TX-R11-consultar-clientes.md)

> GERENTE · Composition MS Cliente + MS Conta · ordenado por nome pt-BR · **sem cache** (tem saldo).

### Request

```http
GET /clientes?busca=Cat HTTP/1.1
Host: localhost:3000
x-access-token: <tokenGerente>
```

_(sem `busca` = todos os clientes)_

### Response 200 — Com filtro `?busca=Cat` (seed)

```json
{
  "clientes": [
    {
      "cpf": "12912861012",
      "nome": "Catharyna",
      "cidade": "Curitiba",
      "estado": "PR",
      "saldo": "800.00",
      "_links": {
        "self":  { "href": "http://localhost:3000/clientes/12912861012" },
        "conta": { "href": "http://localhost:3000/clientes/12912861012/conta" }
      }
    },
    {
      "cpf": "85733854057",
      "nome": "Catianna",
      "cidade": "Curitiba",
      "estado": "PR",
      "saldo": "200.00",
      "_links": {
        "self":  { "href": "http://localhost:3000/clientes/85733854057" },
        "conta": { "href": "http://localhost:3000/clientes/85733854057/conta" }
      }
    }
  ],
  "_links": {
    "self": { "href": "http://localhost:3000/clientes?busca=Cat" }
  }
}
```

> Campos da linha R11: `cpf`, `nome`, `cidade`, `estado` (UF), `saldo` string, `_links.self` e `_links.conta`. Sem `email`/`salario` (esses são do relatório R16).

### Response 200 — Sem filtro, seed completo (pt-BR)

```json
{
  "clientes": [
    {
      "cpf": "12912861012",
      "nome": "Catharyna",
      "cidade": "Curitiba",
      "estado": "PR",
      "saldo": "800.00",
      "_links": {
        "self":  { "href": "http://localhost:3000/clientes/12912861012" },
        "conta": { "href": "http://localhost:3000/clientes/12912861012/conta" }
      }
    },
    {
      "cpf": "85733854057",
      "nome": "Catianna",
      "cidade": "Curitiba",
      "estado": "PR",
      "saldo": "200.00",
      "_links": {
        "self":  { "href": "http://localhost:3000/clientes/85733854057" },
        "conta": { "href": "http://localhost:3000/clientes/85733854057/conta" }
      }
    },
    {
      "cpf": "09506382000",
      "nome": "Cleuddônio",
      "cidade": "Curitiba",
      "estado": "PR",
      "saldo": "10000.00",
      "_links": {
        "self":  { "href": "http://localhost:3000/clientes/09506382000" },
        "conta": { "href": "http://localhost:3000/clientes/09506382000/conta" }
      }
    },
    {
      "cpf": "76179646090",
      "nome": "Coândrya",
      "cidade": "Curitiba",
      "estado": "PR",
      "saldo": "1500.00",
      "_links": {
        "self":  { "href": "http://localhost:3000/clientes/76179646090" },
        "conta": { "href": "http://localhost:3000/clientes/76179646090/conta" }
      }
    },
    {
      "cpf": "58872160006",
      "nome": "Cutardo",
      "cidade": "Curitiba",
      "estado": "PR",
      "saldo": "150000.00",
      "_links": {
        "self":  { "href": "http://localhost:3000/clientes/58872160006" },
        "conta": { "href": "http://localhost:3000/clientes/58872160006/conta" }
      }
    }
  ],
  "_links": {
    "self": { "href": "http://localhost:3000/clientes" }
  }
}
```

---

### Response 503 — saldo ainda não projetado

```json
{
  "status": 503,
  "erro": "Service Unavailable",
  "mensagem": "Saldo ainda não projetado; tente novamente"
}
```

Sem `_links`. O Gateway consulta `/internal/saldos` até três vezes e não preenche `"0.00"`.

### Response 502 — cadastro sem cidade ou UF

```json
{
  "status": 502,
  "erro": "Bad Gateway",
  "mensagem": "Cadastro de cliente sem cidade ou estado"
}
```

Sem `_links`. Teste automatizado: `backend/gateway/test/composition.test.ts` (mock). E2E na frota não força esse estado de forma determinística.

---
