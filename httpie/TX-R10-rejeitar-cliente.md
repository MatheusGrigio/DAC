# TX-R10 — Rejeitar cliente

**ID:** `TX-R10`  
**Requisito:** R10  
**Tipo:** síncrono (200) + e-mail fire-and-forget (`ms.email.cmd`)  
**Diagrama de sequência:** Gerente → Gateway `POST /solicitacoes/{cpf}/rejeicao` → MS Cliente (status `NAO_APROVADA`, motivo, data/hora) → publica e-mail → **200** com a solicitação  
**Pré-requisito:** [TX-R1](./TX-R1-autocadastro.md) ainda `PENDENTE` + `tokenGerente`.

Não use o mesmo CPF em R9 e R10. Se já aprovou `11122233396`, faça reboot + R1 de novo, ou rejeite outro CPF.

---

## Passo a passo no HTTPie Desktop

1. Método **POST**.
2. URL: `{{baseUrl}}/solicitacoes/11122233396/rejeicao`
3. Headers: `x-access-token: {{tokenGerente}}`
4. Body → JSON:

```json
{
  "motivo": "Renda incompatível com a política do banco"
}
```

5. **Send**.

---

## Resposta esperada

**HTTP 200** — `aprovacao` e `rejeicao` **saem** dos `_links`.

```json
{
  "cpf": "11122233396",
  "nome": "Fulano de Tal",
  "email": "fulano@exemplo.com.br",
  "telefone": "41999990000",
  "salario": "4500.00",
  "endereco": {
    "logradouro": "Rua XV de Novembro",
    "numero": "1299",
    "complemento": null,
    "cep": "80060000",
    "cidade": "Curitiba",
    "uf": "PR"
  },
  "status": "NAO_APROVADA",
  "motivo": "Renda incompatível com a política do banco",
  "dataHoraProcessamento": "2026-08-18T16:40:00",
  "_links": {
    "self": { "href": "http://localhost:3000/solicitacoes/11122233396" }
  }
}
```

`dataHoraProcessamento` é gerado no servidor (`America/Sao_Paulo`). Em `MAIL_DEV`, o motivo aparece no outbox do e-mail do candidato.

---

## Casos de erro

**HTTP 409** — rejeitar de novo a mesma solicitação:

```json
{
  "status": 409,
  "erro": "Conflict",
  "mensagem": "Solicitação não está PENDENTE"
}
```

**HTTP 404** — CPF sem solicitação.  
**HTTP 400** — body sem `motivo`.  
**HTTP 403** — token de cliente.
