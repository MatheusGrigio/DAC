# TX-R14 — Atualização de gerente

**ID:** `TX-R14`  
**Requisito:** R14  
**Tipo:** síncrono  
**Diagrama de sequência:** Gerente → Gateway `PUT /gerentes/{cpf}` → MS Gerente (só **nome** e **telefone**) → invalida `cache:gerente:<cpf>` → **200**  
**Pré-requisito:** `tokenGerente`. E-mail (login) e CPF são **imutáveis**. Senha não muda aqui.

---

## Passo a passo no HTTPie Desktop

1. Método **PUT**.
2. URL: `{{baseUrl}}/gerentes/98574307084`
3. Headers: `x-access-token: {{tokenGerente}}`
4. Body → JSON:

```json
{
  "nome": "Geniéve Silva",
  "telefone": "41988889999"
}
```

5. **Send**.

---

## Resposta esperada

**HTTP 200** — e-mail e CPF inalterados. Sem `remocao` no próprio perfil.

```json
{
  "cpf": "98574307084",
  "nome": "Geniéve Silva",
  "email": "ger1@bantads.com.br",
  "telefone": "41988889999",
  "ativo": true,
  "_links": {
    "self": { "href": "http://localhost:3000/gerentes/98574307084" },
    "atualizacao": { "href": "http://localhost:3000/gerentes/98574307084" }
  }
}
```

---

## Casos de erro

**HTTP 400** — tentar mudar e-mail (ou CPF) no corpo:

```json
{
  "nome": "Geniéve Silva",
  "telefone": "41988889999",
  "email": "outro@bantads.com.br"
}
```

```json
{
  "status": 400,
  "erro": "Bad Request",
  "mensagem": "CPF e e-mail são imutáveis"
}
```

**HTTP 404** — CPF inexistente. **HTTP 403** — cliente.
