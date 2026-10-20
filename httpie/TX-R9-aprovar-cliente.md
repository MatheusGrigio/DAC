# TX-R9 — Aprovar cliente (SAGA)

**ID:** `TX-R9`  
**Requisito:** R9  
**Tipo:** assíncrono — SAGA orquestrada (`jobId` = `sagaId`)  
**Diagrama de sequência (sucesso):**  
Gerente → Gateway `POST /solicitacoes/{cpf}/aprovacao` → Redis job `PENDENTE` → `saga.cmd` → Orquestrador  
→ MS Cliente (marca `APROVADA`, devolve dados)  
→ MS Conta (número **aleatório** de 4 dígitos, evento `Criado`, gerente com menos clientes)  
→ MS Auth (usuário CLIENTE + senha aleatória Argon2id)  
→ MS Email fire-and-forget (senha em claro só neste payload)  
→ job Redis `CONCLUIDO` `resultType=resource` `dominio=clientes`  
→ Gerente consulta `GET /clientes/{cpf}`  

**Pré-requisito:** login gerente. **Não** use o CPF `11122233396` se já rejeitou em R10. Fluxo limpo abaixo cria um CPF novo.

O Gateway **não** pré-valida se a solicitação está `PENDENTE`. Inexistente ou já processada → **202** mesmo assim e o job termina `FALHA`.

---

## Passo a passo no HTTPie Desktop

### 1. Autocadastro do candidato (público)

`POST {{baseUrl}}/solicitacoes` — body (e-mail **não** pode ser de gerente/cliente existente):

```json
{
  "cpf": "22233344405",
  "nome": "Beltrano de Tal",
  "email": "beltrano@exemplo.com.br",
  "telefone": "41999990000",
  "salario": "4500.00",
  "endereco": {
    "logradouro": "Rua XV de Novembro",
    "numero": "1299",
    "complemento": null,
    "cep": "80060000",
    "cidade": "Curitiba",
    "uf": "PR"
  }
}
```

Espere **201**.

### 2. Disparar a SAGA

1. Método **POST**.
2. URL: `{{baseUrl}}/solicitacoes/22233344405/aprovacao`
3. Headers: `x-access-token: {{tokenGerente}}`. **Sem body.**
4. Timeout 15 s basta (o 202 é imediato).
5. **Send**.

**HTTP 202 Accepted**  
Header `Location: /jobs/<uuid>/status`  
Sem `_links`.

```json
{
  "jobId": "8f14e45f-ceea-4e1b-9d2a-52f7254b1f0b",
  "status": "PENDENTE"
}
```

Cole o `jobId` na variável `jobId`.

### 3. Polling (transação [TX-JOB-01](./TX-JOB-01-status.md))

`GET {{baseUrl}}/jobs/{{jobId}}/status` com o **mesmo** `tokenGerente`. Repita a cada ~0,5–1 s até `CONCLUIDO` ou `FALHA` (até ~45 s).

**HTTP 200 — sucesso**

```json
{
  "jobId": "8f14e45f-ceea-4e1b-9d2a-52f7254b1f0b",
  "status": "CONCLUIDO",
  "resultType": "resource",
  "dominio": "clientes",
  "resourceId": "22233344405"
}
```

Não use [TX-JOB-02](./TX-JOB-02-result.md) aqui: `resultType` é `resource`, não `inline`.

### 4. Recurso criado

`GET {{baseUrl}}/clientes/22233344405` com `tokenGerente` → **200**, `cpf` = `22233344405`, `_links.self` e `_links.conta`. Número da conta é **aleatório** (não é `2223`).

### 5. Senha (MAIL_DEV)

Com `MAIL_DEV=true`, abra no repo o arquivo `outbox/beltrano@exemplo.com.br.txt` e leia a linha `senha: ....`. Faça [TX-R2A](./TX-R2A-login.md) com esse e-mail e essa senha (`tipo: CLIENTE`).

---

## Casos de falha da SAGA (ainda HTTP 202 no POST)

Aprovar de novo o mesmo CPF, ou CPF inexistente `00000000000`:

Job:

```json
{
  "jobId": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
  "status": "FALHA",
  "erro": "…"
}
```

O campo `erro` é a mensagem do passo que falhou (solicitação não pendente, etc.).

### E-mail já usado por gerente (caso especial)

Autocadastro com e-mail `ger1@bantads.com.br` e CPF novo `33344455516`, depois aprove. Job `FALHA` (`erro` = `"E-mail já cadastrado"`). A solicitação fica `NAO_APROVADA` com motivo automático:

`GET {{baseUrl}}/solicitacoes/33344455516`

```json
{
  "cpf": "33344455516",
  "nome": "Email de Gerente",
  "email": "ger1@bantads.com.br",
  "status": "NAO_APROVADA",
  "motivo": "E-mail já cadastrado",
  "dataHoraProcessamento": "2026-08-18T16:45:01"
}
```

(`dataHoraProcessamento` é o instante real, ISO sem offset.) Compensação: não fica cliente órfão no Auth.

---

## Erros síncronos do POST

**HTTP 401/403** — token ausente/cliente. O POST **não** devolve 404/409 de solicitação: isso vai para o job.
