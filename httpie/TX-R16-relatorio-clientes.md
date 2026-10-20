# TX-R16 — Relatório de clientes (API Composition assíncrona)

**ID:** `TX-R16`  
**Requisito:** R16  
**Tipo:** **202** + job Redis; composition Gateway (Cliente + Conta + Gerente); `resultType=inline`  
**Diagrama de sequência:** Gerente → Gateway `GET /relatorios/clientes` cria job `PENDENTE` e dispara composition em background → job `CONCLUIDO` → `GET /jobs/{id}/result` com a lista (sem `_links` nas linhas) ordenada pt-BR por nome  
**Pré-requisito:** `tokenGerente`. Cliente **403**.

Não é SAGA (não usa `saga.cmd`). TTL do job: 5 min.

---

## Passo a passo no HTTPie Desktop

### 1. Disparar

1. Método **GET** (não POST).
2. URL: `{{baseUrl}}/relatorios/clientes`
3. Headers: `x-access-token: {{tokenGerente}}`
4. Sem body. **Send**.

**HTTP 202**  
`Location: /jobs/<uuid>/status`

```json
{
  "jobId": "bbbbbbbb-cccc-dddd-eeee-ffffffffffff",
  "status": "PENDENTE"
}
```

Sem `cpf` solto no envelope, sem `_links`. Grave `jobId`.

### 2. Polling — [TX-JOB-01](./TX-JOB-01-status.md)

Costuma concluir em **menos de 5 s**.

```json
{
  "jobId": "bbbbbbbb-cccc-dddd-eeee-ffffffffffff",
  "status": "CONCLUIDO",
  "resultType": "inline"
}
```

### 3. Resultado — [TX-JOB-02](./TX-JOB-02-result.md)

`GET {{baseUrl}}/jobs/{{jobId}}/result`

**HTTP 200** — pelo menos os 5 do seed, nesta ordem de nomes. `saldo` é string; `cpfGerente`/`nomeGerente` acompanham a conta (mudam se você rodou R13/R15). Amostra com seed puro:

```json
{
  "clientes": [
    {
      "cpf": "12912861012",
      "nome": "Catharyna",
      "email": "cli1@bantads.com.br",
      "salario": "10000.00",
      "numeroConta": "1291",
      "saldo": "800.00",
      "cpfGerente": "98574307084",
      "nomeGerente": "Geniéve"
    },
    {
      "cpf": "85733854057",
      "nome": "Catianna",
      "email": "cli3@bantads.com.br",
      "salario": "3000.00",
      "numeroConta": "8573",
      "saldo": "200.00",
      "cpfGerente": "23862179060",
      "nomeGerente": "Gyândula"
    },
    {
      "cpf": "09506382000",
      "nome": "Cleuddônio",
      "email": "cli2@bantads.com.br",
      "salario": "20000.00",
      "numeroConta": "0950",
      "saldo": "10000.00",
      "cpfGerente": "64065268052",
      "nomeGerente": "Godophredo"
    },
    {
      "cpf": "76179646090",
      "nome": "Coândrya",
      "email": "cli5@bantads.com.br",
      "salario": "1500.00",
      "numeroConta": "7617",
      "saldo": "1500.00",
      "cpfGerente": "64065268052",
      "nomeGerente": "Godophredo"
    },
    {
      "cpf": "58872160006",
      "nome": "Cutardo",
      "email": "cli4@bantads.com.br",
      "salario": "500.00",
      "numeroConta": "5887",
      "saldo": "150000.00",
      "cpfGerente": "98574307084",
      "nomeGerente": "Geniéve"
    }
  ]
}
```

Se [TX-R14](./TX-R14-atualizar-gerente.md) já rodou, `nomeGerente` de Catharyna/Cutardo pode ser `"Geniéve Silva"`.

---

## Erros

**HTTP 403** — `tokenCliente` no GET `/relatorios/clientes`:

```json
{
  "status": 403,
  "erro": "Forbidden",
  "mensagem": "Acesso negado"
}
```
