# TX-R2B — Logout (R2)

**ID:** `TX-R2B`  
**Requisito:** R2  
**Tipo:** síncrono, autenticado  
**Diagrama de sequência:** Tester → Gateway `POST /logout` (JWT + sessão) → Redis: revoga `jti`, `DEL sessao:<jti>` e `DEL sessao:cpf:<cpf>` → **204** sem corpo  
**Pré-requisito:** [TX-R2A](./TX-R2A-login.md) com `tokenCliente` (ou gerente) preenchido.

---

## Passo a passo no HTTPie Desktop

1. Método **POST**.
2. URL: `{{baseUrl}}/logout`
3. Headers:

| Header | Valor |
|---|---|
| `Accept` | `application/json` |
| `x-access-token` | `{{tokenCliente}}` |

4. Sem body. **Send**.

---

## Resposta esperada

**HTTP 204 No Content** — corpo vazio. Sem `_links`.

---

## Provar que a sessão morreu

Repita um GET autenticado com o **mesmo** token, por exemplo [TX-CAD-01](./TX-CAD-01-consultar-cliente.md):

```
GET {{baseUrl}}/clientes/12912861012
x-access-token: {{tokenCliente}}
```

**HTTP 401**

```json
{
  "auth": false,
  "message": "Falha ao autenticar o token."
}
```

Faça [TX-R2A](./TX-R2A-login.md) de novo para continuar testando.
