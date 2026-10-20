# TX-R13 — Inserção de gerente (SAGA)

**ID:** `TX-R13`  
**Requisito:** R13  
**Tipo:** assíncrono — SAGA orquestrada (`jobId` = `sagaId`)  
**Diagrama de sequência (sucesso):** Gerente → Gateway `POST /gerentes` → job Redis → `saga.cmd`  
→ MS Gerente cria ativo  
→ MS Auth cria login (senha **do formulário**, não vai por e-mail)  
→ MS Conta identifica conta a transferir (gerente com **mais** contas; empate → menor soma de saldos; conta de **menor** saldo desse gerente). Se todos têm ≤1 conta, **nenhuma** conta sai (nunca deixa um gerente existente com 0).  
→ (se houver conta) MS Conta `GerenteAlterado` + MS Cliente consulta + MS Email troca de gerente (FF)  
→ job `CONCLUIDO` `resultType=resource` `dominio=gerentes`  
→ `GET /gerentes/{cpf}`  

**Pré-requisito:** reboot + `tokenGerente`. Unicidade de e-mail é do MS Auth **dentro** da SAGA (duplicata → job `FALHA`, 202 no POST).

---

## Passo a passo no HTTPie Desktop

### 1. Disparar a SAGA

1. Método **POST**.
2. URL: `{{baseUrl}}/gerentes`
3. Headers: `x-access-token: {{tokenGerente}}`
4. Body → JSON (CPF/e-mail **novos**):

```json
{
  "cpf": "55667788990",
  "nome": "Gumercindo",
  "email": "ger5@bantads.com.br",
  "telefone": "41988880005",
  "senha": "tads"
}
```

5. **Send**.

**HTTP 202**  
`Location: /jobs/<uuid>/status`

```json
{
  "jobId": "11111111-2222-3333-4444-555555555555",
  "status": "PENDENTE"
}
```

Grave `jobId`. Sem `_links`. Sem `senha` na resposta.

### 2. Polling — [TX-JOB-01](./TX-JOB-01-status.md)

**HTTP 200 — sucesso**

```json
{
  "jobId": "11111111-2222-3333-4444-555555555555",
  "status": "CONCLUIDO",
  "resultType": "resource",
  "dominio": "gerentes",
  "resourceId": "55667788990"
}
```

### 3. Recurso

`GET {{baseUrl}}/gerentes/55667788990`

```json
{
  "cpf": "55667788990",
  "nome": "Gumercindo",
  "email": "ger5@bantads.com.br",
  "telefone": "41988880005",
  "ativo": true,
  "_links": {
    "self": { "href": "http://localhost:3000/gerentes/55667788990" },
    "atualizacao": { "href": "http://localhost:3000/gerentes/55667788990" },
    "remocao": { "href": "http://localhost:3000/gerentes/55667788990" }
  }
}
```

Login: `ger5@bantads.com.br` / `tads` ([TX-R2A](./TX-R2A-login.md)).

### 4. Conta transferida no seed

No seed, Geniéve e Godophredo empatam com 2 contas; Godophredo tem menor soma de saldos; a conta de menor saldo dele é **Coândrya `7617`**.

`GET {{baseUrl}}/contas/7617` (token gerente). Após a projeção CQRS: `"cpfGerente": "55667788990"`. Se ainda for Godophredo, espere 2–5 s e consulte de novo.

---

## Falha: e-mail duplicado

Body com `"email": "ger1@bantads.com.br"` e CPF novo `77889900112` → POST **202**, job `FALHA` (`erro`: `"E-mail já cadastrado"`). `GET /gerentes/77889900112` → **404** (compensação; não fica órfão).

---

## Erros síncronos

**HTTP 400** — CPF/e-mail/senha ausentes. **HTTP 403** — cliente. O 202 **não** antecipa e-mail duplicado.
