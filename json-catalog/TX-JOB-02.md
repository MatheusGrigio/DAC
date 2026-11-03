## TX-JOB-02 — Resultado inline do job

> Voltar ao [índice do catálogo](../00-JSON-CATALOG.md).


**Tutorial:** [transacoes/TX-JOB-02-result.md](../transacoes/TX-JOB-02-result.md)

> Só para `resultType = "inline"` + `status = "CONCLUIDO"` · sem `_links`.

### Request

```http
GET /jobs/aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee/result HTTP/1.1
Host: localhost:3000
x-access-token: <mesmo token>
```

### Response 200 — Resultado R15 (remoção de gerente)

```json
{
  "mensagem": "Gerente removido; 0 contas transferidas para Gyândula"
}
```

### Response 200 — Resultado R16 (relatório)

> Mesma estrutura de `GET /jobs/{id}/result` descrita em [TX-R16](TX-R16.md).

```json
{
  "clientes": [ ... ]
}
```

### Erros

**409 — Job não concluído, FALHA ou `resultType = "resource"` (não inline):**

```json
{
  "status": 409,
  "erro": "Conflict",
  "mensagem": "Job ainda não concluído, falhou ou não é inline"
}
```

**404 — Expirado ou inexistente.**

---
