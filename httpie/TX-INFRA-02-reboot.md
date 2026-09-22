# TX-INFRA-02 — Reboot (recriar o seed)

**ID:** `TX-INFRA-02`  
**Requisito:** apoio à correção / estado conhecido (seção 4 do enunciado)  
**Tipo:** síncrono, público  
**Diagrama de sequência:** Tester → Gateway `POST /reboot` → MS Cliente, MS Conta (command+query), MS Gerente, MS Auth (limpa solicitações, jobs e sessões) → 200 com contagens  
**Pré-requisito:** [TX-INFRA-01](./TX-INFRA-01-health.md). Sem token.

É a **primeira chamada de negócio** antes dos tutoriais. Invalida tokens já gravados no HTTPie (sessões Redis apagadas). Depois do reboot, faça login de novo ([TX-R2A](./TX-R2A-login.md)).

---

## Passo a passo no HTTPie Desktop

1. Timeout do request: **90 segundos** (Settings da request). O reboot sincroniza vários bancos.
2. Método **POST**.
3. URL:

```
{{baseUrl}}/reboot
```

4. Headers: `Accept: application/json`. Sem token. Sem body.
5. **Send**. Pode demorar na primeira vez após o `up`.

---

## Resposta esperada

**HTTP 200** — corpo **idêntico** na primeira e na segunda chamada. Sem `_links`.

```json
{
  "status": "ok",
  "clientes": 5,
  "gerentes": 4,
  "contas": 5
}
```

Saldos após reboot (lado query do CQRS, iguais ao replay do event store):

| Conta | Cliente | Saldo |
|---|---|---|
| `1291` | Catharyna | `"800.00"` |
| `0950` | Cleuddônio | `"10000.00"` |
| `8573` | Catianna | `"200.00"` |
| `5887` | Cutardo | `"150000.00"` |
| `7617` | Coândrya | `"1500.00"` |

---

## Depois

1. Apague `tokenCliente` / `tokenGerente` do environment (ou sobrescreva no próximo login).
2. Siga para [TX-R2A-login.md](./TX-R2A-login.md).
