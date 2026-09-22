# TX-R2A — Login (R2)

**ID:** `TX-R2A`  
**Requisito:** R2  
**Tipo:** síncrono, público  
**Diagrama de sequência:** Tester → Gateway `POST /login` → MS Auth (`/auth/verificar`, Argon2id) → Gateway busca nome/e-mail no MS Cliente ou MS Gerente → assina JWT → grava sessão Redis (`sessao:<jti>` + `sessao:cpf:<cpf>`) → `{ auth, token, tipo, usuario }`  
**Pré-requisito:** [TX-INFRA-02](./TX-INFRA-02-reboot.md).

Sem `_links`. O token vai no header `x-access-token` de todas as rotas autenticadas.

---

## Passo a passo no HTTPie Desktop

### A) Login de cliente (Catharyna)

1. Método **POST**.
2. URL: `{{baseUrl}}/login`
3. Headers: `Accept: application/json`
4. Body → JSON:

```json
{
  "email": "cli1@bantads.com.br",
  "senha": "tads"
}
```

5. **Send**.

**HTTP 200**

```json
{
  "auth": true,
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tipo": "CLIENTE",
  "usuario": {
    "cpf": "12912861012",
    "nome": "Catharyna",
    "email": "cli1@bantads.com.br"
  }
}
```

6. Copie `token` para a variável de environment `tokenCliente`.

### B) Login de gerente (Geniéve)

Mesmo request, body:

```json
{
  "email": "ger1@bantads.com.br",
  "senha": "tads"
}
```

**HTTP 200** — `tipo` = `"GERENTE"`, `usuario.cpf` = `"98574307084"`, `usuario.nome` = `"Geniéve"`. Grave em `tokenGerente`.

Repita o padrão para `cli2`…`cli5` e `ger2`…`ger4` (senha sempre `tads`). Lista: [00-GENERAL-INFO](./00-GENERAL-INFO.md).

---

## Casos de erro

**HTTP 401** — senha errada ou usuário inativo (ex.: gerente removido em R15):

```json
{
  "auth": false,
  "message": "Login inválido!"
}
```

Body de exemplo (falha):

```json
{
  "email": "cli1@bantads.com.br",
  "senha": "errada"
}
```

**HTTP 400** — JSON sem `email`/`senha` ou malformado:

```json
{
  "status": 400,
  "erro": "Bad Request",
  "mensagem": "Requisição malformada"
}
```

---

## Sem token vs token inválido (próximas rotas)

Chame `GET {{baseUrl}}/clientes/12912861012` **sem** header:

```json
{ "auth": false, "message": "Token não fornecido." }
```

Com `x-access-token: nao.e.jwt`:

```json
{ "auth": false, "message": "Falha ao autenticar o token." }
```
