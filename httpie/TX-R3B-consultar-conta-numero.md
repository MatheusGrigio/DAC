# TX-R3B — Consultar conta pelo número

**ID:** `TX-R3B`  
**Requisito:** R3 (recurso `self` da conta)  
**Tipo:** consulta CQRS query  
**Diagrama de sequência:** CLIENTE dono ou GERENTE → Gateway `GET /contas/{numero}` → MS Conta query  
**Pré-requisito:** `tokenCliente` (Catharyna) ou `tokenGerente`.

Mesma representação de [TX-R3A](./TX-R3A-consultar-conta-cpf.md). Use depois de R4/R5/R6 para ver o saldo projetado.

---

## Passo a passo no HTTPie Desktop

1. Método **GET**.
2. URL (zero à esquerda obrigatório como string de path):

```
{{baseUrl}}/contas/1291
```

Conta do Cleuddônio: `{{baseUrl}}/contas/0950` (com token de `cli2` ou gerente).

3. Headers: `x-access-token: {{tokenCliente}}`
4. **Send**.

---

## Resposta esperada (seed, dono)

**HTTP 200** — idêntico ao JSON de TX-R3A (`numero: "1291"`, `saldo: "800.00"`, links de escrita se o perfil for CLIENTE).

Gerente: **200** sem links de movimento.

---

## Erros

**HTTP 403** — `cli2` em `/contas/1291`.  
**HTTP 404** — `/contas/0001`:

```json
{
  "status": 404,
  "erro": "Not Found",
  "mensagem": "Conta não encontrada"
}
```
