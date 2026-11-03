## TX-R2B — Logout

> Voltar ao [índice do catálogo](../00-JSON-CATALOG.md).


**Tutorial:** [transacoes/TX-R2B-logout.md](../transacoes/TX-R2B-logout.md)

> Qualquer JWT válido · body vazio no response · sem `_links`.

### Request

```http
POST /logout HTTP/1.1
Host: localhost:3000
x-access-token: <token>
```

_(sem body)_

### Response 204 — Sessão encerrada

```
HTTP/1.1 204 No Content
```

_(corpo vazio — não é `{}`)_

### Após logout — mesmo token → 401

```json
{
  "auth": false,
  "message": "Falha ao autenticar o token."
}
```

---
