# TX-R11 — Consultar todos os clientes (API Composition)

**ID:** `TX-R11`  
**Requisito:** R11  
**Tipo:** API Composition **síncrona** no Gateway (MS Cliente + MS Conta query)  
**Diagrama de sequência:** Gerente → Gateway `GET /clientes?busca=` → paralelo/sequência Cliente (cadastro) + Conta (saldos) → ordena collation **pt-BR** por nome → lista `ClienteResumo`  
**Pré-requisito:** `tokenGerente`. Não cacheia saldo.

`busca` opcional: trecho de CPF **ou** nome (ex.: `Cat` → Catharyna e Catianna).

---

## Passo a passo no HTTPie Desktop

### Com filtro (caso dos testes)

1. Método **GET**.
2. URL:

```
{{baseUrl}}/clientes?busca=Cat
```

3. Headers: `x-access-token: {{tokenGerente}}`
4. **Send**.

**HTTP 200** — ordem crescente por nome. Saldos abaixo são os do **seed** (mude se você depositou/sacou).

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
        "self": { "href": "http://localhost:3000/clientes/12912861012" },
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
        "self": { "href": "http://localhost:3000/clientes/85733854057" },
        "conta": { "href": "http://localhost:3000/clientes/85733854057/conta" }
      }
    }
  ],
  "_links": {
    "self": { "href": "http://localhost:3000/clientes?busca=Cat" }
  }
}
```

Cleuddônio **não** entra nesse filtro.

### Sem busca (todos)

```
{{baseUrl}}/clientes
```

Ordem pt-BR do seed: Catharyna, Catianna, Cleuddônio, Coândrya, Cutardo. Cada linha tem `cidade`, `estado`, `saldo` string `^\d+\.\d{2}$`.

---

## Erros

**HTTP 403** com `tokenCliente`. **HTTP 401** sem token.
