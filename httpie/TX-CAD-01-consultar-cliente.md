# TX-CAD-01 — Consultar dados cadastrais do cliente

**ID:** `TX-CAD-01`  
**Requisito:** cadastro (recurso apontado pelo job R9: `dominio=clientes`)  
**Tipo:** consulta síncrona; cache Gateway `cache:cliente:<cpf>` TTL 5 min (invalidado na aprovação R9)  
**Diagrama de sequência:** CLIENTE próprio ou GERENTE → Gateway (cache-aside) → MS Cliente `GET /clientes/{cpf}`  
**Pré-requisito:** login. Catharyna só lê o próprio CPF; gerente lê qualquer cliente.

---

## Passo a passo no HTTPie Desktop

1. Método **GET**.
2. URL: `{{baseUrl}}/clientes/12912861012`
3. Headers: `x-access-token: {{tokenGerente}}` (ou `{{tokenCliente}}` se for a própria Catharyna).
4. **Send**.

---

## Resposta esperada

**HTTP 200**

```json
{
  "cpf": "12912861012",
  "nome": "Catharyna",
  "email": "cli1@bantads.com.br",
  "telefone": "41999990001",
  "salario": "10000.00",
  "endereco": {
    "logradouro": "Rua XV de Novembro",
    "numero": "1299",
    "complemento": null,
    "cep": "80060000",
    "cidade": "Curitiba",
    "uf": "PR"
  },
  "_links": {
    "self": { "href": "http://localhost:3000/clientes/12912861012" },
    "conta": { "href": "http://localhost:3000/clientes/12912861012/conta" }
  }
}
```

Não há saldo neste DTO (saldo está na conta, TX-R3).

---

## Erros

**HTTP 403** — `tokenCliente` de Catharyna em `/clientes/09506382000`.  
**HTTP 404** — CPF que não é cliente (solicitação pendente ainda não aprovada): `"Cliente não encontrado"`.  
**HTTP 401** — token ausente: `{ "auth": false, "message": "Token não fornecido." }`
