## TX-R2A — Login

> Voltar ao [índice do catálogo](../00-JSON-CATALOG.md).


**Tutorial:** [transacoes/TX-R2A-login.md](../transacoes/TX-R2A-login.md)

> Público · sem `_links` · campo chama-se `email`, não `login`.

### Request

```http
POST /login HTTP/1.1
Host: localhost:3000
Content-Type: application/json
```

```json
{
  "email": "cli1@bantads.com.br",
  "senha": "tads"
}
```

### Response 200 — Autenticado (CLIENTE)

> Sem `_links`. `token` vai no header `x-access-token` de todas as requests seguintes.

```json
{
  "auth": true,
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJjcGYiOiIxMjkxMjg2MTAxMiIsInRpcG8iOiJDTElFTlRFIiwianRpIjoiYWJjZGVmZ2giLCJleHAiOjE3NTU1NTU1NTV9.assinatura",
  "tipo": "CLIENTE",
  "usuario": {
    "cpf": "12912861012",
    "nome": "Catharyna",
    "email": "cli1@bantads.com.br"
  }
}
```

### Response 200 — Autenticado (GERENTE)

```json
{
  "auth": true,
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tipo": "GERENTE",
  "usuario": {
    "cpf": "98574307084",
    "nome": "Geniéve",
    "email": "ger1@bantads.com.br"
  }
}
```

### Erros

**401 — Credenciais inválidas ou usuário inativo:**

```json
{
  "auth": false,
  "message": "Login inválido!"
}
```

**400 — Body malformado (sem `email`/`senha` ou tipos errados):**

```json
{
  "status": 400,
  "erro": "Bad Request",
  "mensagem": "Requisição malformada"
}
```

**401 — Token ausente em rota protegida:**

```json
{
  "auth": false,
  "message": "Token não fornecido."
}
```

**401 — Token inválido/expirado/sessão encerrada:**

```json
{
  "auth": false,
  "message": "Falha ao autenticar o token."
}
```

**Seed de clientes e gerentes:**

| Email | Senha | Tipo | CPF | Nome |
|---|---|---|---|---|
| `cli1@bantads.com.br` | `tads` | CLIENTE | `12912861012` | Catharyna |
| `cli2@bantads.com.br` | `tads` | CLIENTE | `09506382000` | Cleuddônio |
| `cli3@bantads.com.br` | `tads` | CLIENTE | `85733854057` | Catianna |
| `cli4@bantads.com.br` | `tads` | CLIENTE | `58872160006` | Cutardo |
| `cli5@bantads.com.br` | `tads` | CLIENTE | `76179646090` | Coândrya |
| `ger1@bantads.com.br` | `tads` | GERENTE | `98574307084` | Geniéve |
| `ger2@bantads.com.br` | `tads` | GERENTE | `64065268052` | Godophredo |
| `ger3@bantads.com.br` | `tads` | GERENTE | `23862179060` | Gyândula |
| `ger4@bantads.com.br` | `tads` | GERENTE | `40501740066` | Gadamântio |

---
