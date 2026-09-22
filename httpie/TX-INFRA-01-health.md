# TX-INFRA-01 — Health check do Gateway

**ID:** `TX-INFRA-01`  
**Requisito:** infra (não é R1–R16)  
**Tipo:** síncrono, público  
**Diagrama de sequência:** Tester → API Gateway (`GET /health`) → 200 `{ status: UP }`  
**Pré-requisito:** [00-GENERAL-INFO](./00-GENERAL-INFO.md) (frota no ar). Sem token.

Usado para saber se o Gateway está pronto antes de qualquer outra transação. Sem `_links`.

---

## Passo a passo no HTTPie Desktop

1. Environment `BANTADS Local` selecionado.
2. Nova request na collection `BANTADS`, nome `TX-INFRA-01 health`.
3. Método **GET**.
4. URL:

```
{{baseUrl}}/health
```

5. Headers:

| Header | Valor |
|---|---|
| `Accept` | `application/json` |

6. Sem body. Sem `x-access-token`.
7. **Send**.

---

## Resposta esperada

**HTTP 200**

```json
{
  "status": "UP"
}
```

Não existe campo `_links`. Qualquer outro JSON (ou timeout) indica que o Gateway ainda não está healthy — espere e repita, ou rode `docker compose ps`.
