# TX-R12 — Listagem de gerentes (API Composition)

**ID:** `TX-R12`  
**Requisito:** R12  
**Tipo:** API Composition síncrona (MS Gerente ativos + MS Conta query para `quantidadeClientes`)  
**Diagrama de sequência:** Gerente → Gateway `GET /gerentes` → Gerente (ativos) + Conta (contagens) → ordenação pt-BR por nome  
**Pré-requisito:** reboot + `tokenGerente` de Geniéve (`98574307084`).

Só gerentes **ativos**. HATEOAS: a lista tem `criacao`; cada item tem `atualizacao`; `remocao` **some** no próprio CPF autenticado (não dá para se auto-remover pela UI).

---

## Passo a passo no HTTPie Desktop

1. Método **GET**.
2. URL: `{{baseUrl}}/gerentes`
3. Headers: `x-access-token: {{tokenGerente}}`
4. **Send**.

---

## Resposta esperada (seed)

**HTTP 200** — 4 gerentes. Ordem de nomes: Gadamântio, Geniéve, Godophredo, Gyândula.

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
        "self": { "href": "http://localhost:3000/gerentes/40501740066" },
        "atualizacao": { "href": "http://localhost:3000/gerentes/40501740066" },
        "remocao": { "href": "http://localhost:3000/gerentes/40501740066" }
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
        "self": { "href": "http://localhost:3000/gerentes/98574307084" },
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
        "self": { "href": "http://localhost:3000/gerentes/64065268052" },
        "atualizacao": { "href": "http://localhost:3000/gerentes/64065268052" },
        "remocao": { "href": "http://localhost:3000/gerentes/64065268052" }
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
        "self": { "href": "http://localhost:3000/gerentes/23862179060" },
        "atualizacao": { "href": "http://localhost:3000/gerentes/23862179060" },
        "remocao": { "href": "http://localhost:3000/gerentes/23862179060" }
      }
    }
  ],
  "_links": {
    "self": { "href": "http://localhost:3000/gerentes" },
    "criacao": { "href": "http://localhost:3000/gerentes" }
  }
}
```

Se você já rodou [TX-R14](./TX-R14-atualizar-gerente.md), o nome de Geniéve pode ser `"Geniéve Silva"` (continua na 2ª posição da collation).

---

## Erros

**HTTP 403** — token de cliente. **HTTP 401** — sem token.
