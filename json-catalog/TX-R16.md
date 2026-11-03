## TX-R16 — Relatório de clientes

> Voltar ao [índice do catálogo](../00-JSON-CATALOG.md).


**Tutorial:** [transacoes/TX-R16-relatorio-clientes.md](../transacoes/TX-R16-relatorio-clientes.md)

> GERENTE · **GET** (não POST) · composition assíncrona sem `saga.cmd` · resultado inline · sem `_links` nas linhas.

### Request

```http
GET /relatorios/clientes HTTP/1.1
Host: localhost:3000
x-access-token: <tokenGerente>
```

### Response 202 — Job aceito

Header: `Location: /jobs/bbbbbbbb-cccc-dddd-eeee-ffffffffffff/status`

```json
{
  "jobId": "bbbbbbbb-cccc-dddd-eeee-ffffffffffff",
  "status": "PENDENTE"
}
```

### Polling → CONCLUIDO inline

```json
{
  "jobId": "bbbbbbbb-cccc-dddd-eeee-ffffffffffff",
  "status": "CONCLUIDO",
  "resultType": "inline"
}
```

### GET /jobs/{jobId}/result — Lista de clientes

> Ordenada pt-BR por nome. Sem `_links` em nenhum lugar do envelope. `saldo` e `salario` são `string`.

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

### GET /jobs/{jobId}/result — Lista após T09 (6 clientes)

> Inclui Beltrano aprovado na suíte aceite (`test_t09`).

```json
{
  "clientes": [
    {
      "cpf": "22233344405",
      "nome": "Beltrano de Tal",
      "email": "beltrano@exemplo.com.br",
      "salario": "4500.00",
      "numeroConta": "0000",
      "saldo": "0.00",
      "cpfGerente": "98574307084",
      "nomeGerente": "Geniéve Silva"
    },
    {
      "cpf": "12912861012",
      "nome": "Catharyna",
      "email": "cli1@bantads.com.br",
      "salario": "10000.00",
      "numeroConta": "1291",
      "saldo": "800.00",
      "cpfGerente": "98574307084",
      "nomeGerente": "Geniéve Silva"
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
      "nomeGerente": "Geniéve Silva"
    }
  ]
}
```

### Erros

**403 — CLIENTE autenticado:**

```json
{
  "status": 403,
  "erro": "Forbidden",
  "mensagem": "Acesso negado"
}
```

---
