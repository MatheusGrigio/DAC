## TX-INFRA-02 — Reboot

> Voltar ao [índice do catálogo](../00-JSON-CATALOG.md).


**Tutorial:** [transacoes/TX-INFRA-02-reboot.md](../transacoes/TX-INFRA-02-reboot.md)

> Público · timeout ≥ 90 s · idempotente · invalida todas as sessões Redis.

### Request

```http
POST /reboot HTTP/1.1
Host: localhost:3000
```

_(sem body)_

### Response 200 — Seed recriado

> Sem `_links`. Resposta **byte-a-byte idêntica** em chamadas subsequentes.

```json
{
  "status": "ok",
  "clientes": 5,
  "gerentes": 4,
  "contas": 5
}
```

**Saldos do seed após reboot:**

| Conta | Cliente | Saldo |
|---|---|---|
| `"1291"` | Catharyna | `"800.00"` |
| `"0950"` | Cleuddônio | `"10000.00"` |
| `"8573"` | Catianna | `"200.00"` |
| `"5887"` | Cutardo | `"150000.00"` |
| `"7617"` | Coândrya | `"1500.00"` |

---
