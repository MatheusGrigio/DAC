# TX-JOB-02 — Resultado inline do job

**ID:** `TX-JOB-02`  
**Requisito:** seção 5.8 — só jobs `resultType=inline` (**R15** mensagem; **R16** lista)  
**Tipo:** leitura Redis; **sem** `_links`  
**Diagrama de sequência:** dono do job → Gateway `GET /jobs/{jobId}/result` → se `CONCLUIDO` + inline, devolve `resultado`; senão **409**  
**Pré-requisito:** [TX-JOB-01](./TX-JOB-01-status.md) já em `CONCLUIDO` com `resultType=inline`.

Não use este endpoint após R9/R13 (`resource`): o recurso está em `GET /clientes/{cpf}` ou `GET /gerentes/{cpf}`.

---

## Passo a passo no HTTPie Desktop

1. Método **GET**.
2. URL: `{{baseUrl}}/jobs/{{jobId}}/result`
3. Headers: `x-access-token` do dono (`{{tokenGerente}}`).
4. **Send**.

---

## Respostas esperadas

### Depois de R15

**HTTP 200**

```json
{
  "mensagem": "Gerente removido; 0 contas transferidas para Gyândula"
}
```

(Contagem e nome do destino mudam se o gerente removido tinha contas.)

### Depois de R16

**HTTP 200** — objeto `{ "clientes": [ ... ] }` como em [TX-R16](./TX-R16-relatorio-clientes.md). Cada linha **sem** `_links`.

---

## Erros

**HTTP 409** — job `PENDENTE`, `FALHA`, ou `resultType=resource`:

```json
{
  "status": 409,
  "erro": "Conflict",
  "mensagem": "Job ainda não concluído, falhou ou não é inline"
}
```

**HTTP 404** — expirado (5 min) ou id inválido: `"Job inexistente ou expirado"`.  
**HTTP 403** — `"Job não pertence ao usuário autenticado"`.
