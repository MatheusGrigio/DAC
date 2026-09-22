# TX-CAD-02 — Consultar um gerente

**ID:** `TX-CAD-02`  
**Requisito:** recurso apontado pelo job R13 (`dominio=gerentes`)  
**Tipo:** consulta síncrona; cache Gateway `cache:gerente:<cpf>` TTL 5 min (invalidado em R13/R14/R15)  
**Diagrama de sequência:** Gerente → Gateway → MS Gerente `GET /gerentes/{cpf}`  
**Pré-requisito:** `tokenGerente`. `quantidadeClientes` pode vir **nula** neste recurso (a contagem vive na listagem R12).

---

## Passo a passo no HTTPie Desktop

1. Método **GET**.
2. URL: `{{baseUrl}}/gerentes/98574307084`
3. Headers: `x-access-token: {{tokenGerente}}`
4. **Send**.

---

## Resposta esperada (o próprio Geniéve autenticado)

**HTTP 200** — **sem** `remocao` (não pode remover a si mesmo).

```json
{
  "cpf": "98574307084",
  "nome": "Geniéve",
  "email": "ger1@bantads.com.br",
  "telefone": "41988880001",
  "ativo": true,
  "_links": {
    "self": { "href": "http://localhost:3000/gerentes/98574307084" },
    "atualizacao": { "href": "http://localhost:3000/gerentes/98574307084" }
  }
}
```

`GET {{baseUrl}}/gerentes/40501740066` (Gadamântio, outra pessoa): inclui `"remocao"` no `_links`.

---

## Erros

**HTTP 404** — CPF inexistente. **HTTP 403** — cliente. **HTTP 401** — sem token.
