# TX-R8B — Consultar uma solicitação

**ID:** `TX-R8B`  
**Requisito:** apoio a R8 / R9 / R10 (recurso `GET /solicitacoes/{cpf}`)  
**Tipo:** consulta síncrona  
**Diagrama de sequência:** Gerente → Gateway → MS Cliente → `Solicitacao`  
**Pré-requisito:** [TX-R1](./TX-R1-autocadastro.md) + login gerente.

---

## Passo a passo no HTTPie Desktop

1. Método **GET**.
2. URL:

```
{{baseUrl}}/solicitacoes/11122233396
```

3. Headers: `Accept: application/json` e `x-access-token: {{tokenGerente}}`.
4. **Send**.

---

## Resposta esperada (ainda PENDENTE)

**HTTP 200** — mesmo JSON de um item da lista R8 (ver [TX-R8A](./TX-R8A-listar-solicitacoes.md)).

---

## Erros

**HTTP 404** — CPF sem solicitação (`00000000000`):

```json
{
  "status": 404,
  "erro": "Not Found",
  "mensagem": "Solicitação não encontrada"
}
```

**HTTP 403** com token de cliente. **HTTP 401** sem token.
