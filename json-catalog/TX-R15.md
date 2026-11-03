## TX-R15 — Remover gerente (SAGA)

> Voltar ao [índice do catálogo](../00-JSON-CATALOG.md).


**Tutorial:** [transacoes/TX-R15-remover-gerente.md](../transacoes/TX-R15-remover-gerente.md)

> GERENTE · resultado **inline** · auto-remoção = **403 síncrono** (sem job).

### Request — Auto-remoção (403 síncrono)

```http
DELETE /gerentes/98574307084 HTTP/1.1
Host: localhost:3000
x-access-token: <tokenGerente>   (Geniéve, CPF 98574307084)
```

**Response 403 — sem job:**

```json
{
  "status": 403,
  "erro": "Forbidden",
  "mensagem": "Não é permitido remover a si mesmo"
}
```

### Request — Remoção de outro gerente

```http
DELETE /gerentes/40501740066 HTTP/1.1
Host: localhost:3000
x-access-token: <tokenGerente>
```

### Response 202 — Job aceito

Header: `Location: /jobs/aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee/status`

```json
{
  "jobId": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
  "status": "PENDENTE"
}
```

### Polling → CONCLUIDO inline

```json
{
  "jobId": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
  "status": "CONCLUIDO",
  "resultType": "inline"
}
```

> `resultType = "inline"` → `GET /jobs/{jobId}/result`.

### GET /jobs/{jobId}/result — Resultado

```json
{
  "mensagem": "Gerente removido; 0 contas transferidas para Gyândula"
}
```

> Gadamântio no seed tem 0 contas; destino é o ativo com menos clientes (Gyândula=1). Com contas: `"Gerente removido; N contas transferidas para {Nome}"`.

### Login do removido → 401

`POST /login` com `ger4@bantads.com.br` / `tads`:

```json
{
  "auth": false,
  "message": "Login inválido!"
}
```

### Job FALHA (já inativo ou CPF inexistente)

```json
{
  "jobId": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
  "status": "FALHA",
  "erro": "Gerente não encontrado ou já inativo"
}
```

### Job FALHA — Último gerente ativo

```json
{
  "jobId": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
  "status": "FALHA",
  "erro": "Não é permitido remover o último gerente ativo"
}
```

---
