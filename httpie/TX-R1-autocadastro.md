# TX-R1 — Autocadastro (solicitação de conta)

**ID:** `TX-R1`  
**Requisito:** R1  
**Tipo:** síncrono, público (grava solicitação `PENDENTE`; **não** cria conta, usuário nem senha)  
**Diagrama de sequência:** Candidato → Gateway `POST /solicitacoes` → MS Cliente (unicidade CPF/e-mail na tabela de solicitações) → **201** + `Location`  
**Pré-requisito:** [TX-INFRA-02](./TX-INFRA-02-reboot.md). Sem token.

Um CPF só pode ter uma solicitação (qualquer status). E-mail também único entre solicitações. Conta/Auth só nascem em [TX-R9](./TX-R9-aprovar-cliente.md).

---

## Passo a passo no HTTPie Desktop

1. Método **POST**.
2. URL: `{{baseUrl}}/solicitacoes`
3. Headers: `Accept: application/json`
4. Body → JSON (use um CPF que **não** esteja no seed):

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
  }
}
```

5. **Send**.

---

## Resposta esperada

**HTTP 201 Created**  
Header `Location: /solicitacoes/11122233396`

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
  "status": "PENDENTE",
  "motivo": null,
  "dataHoraProcessamento": null,
  "_links": {
    "self": { "href": "http://localhost:3000/solicitacoes/11122233396" },
    "aprovacao": { "href": "http://localhost:3000/solicitacoes/11122233396/aprovacao" },
    "rejeicao": { "href": "http://localhost:3000/solicitacoes/11122233396/rejeicao" }
  }
}
```

Os links `aprovacao` e `rejeicao` só existem enquanto `PENDENTE` (HATEOAS dirigindo a tela do gerente).

---

## Casos de erro

**HTTP 409** — mesmo CPF de novo, ou mesmo e-mail com outro CPF:

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
  }
}
```

Corpo típico (CPF duplicado):

```json
{
  "status": 409,
  "erro": "Conflict",
  "mensagem": "CPF já possui solicitação"
}
```

E-mail já usado: `"E-mail já usado em outra solicitação"`. CPF que **já é cliente** do seed (`12912861012`): `"CPF já possui cadastro de cliente"`.

**HTTP 400** — CPF/CEP/UF/salário fora do padrão (ex.: `"salario": 4500` number, ou CPF com pontuação).

---

## Próximo

- Listar: [TX-R8A](./TX-R8A-listar-solicitacoes.md)  
- Aprovar (SAGA): [TX-R9](./TX-R9-aprovar-cliente.md) — use **outro** CPF (`22233344405`) se já rejeitou este  
- Rejeitar: [TX-R10](./TX-R10-rejeitar-cliente.md)
