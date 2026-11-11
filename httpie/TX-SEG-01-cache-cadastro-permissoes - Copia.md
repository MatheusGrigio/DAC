# TX-SEG-01 - Cache de cadastro e permissoes

**ID:** `TX-SEG-01`  
**Tipo:** teste manual de regressao no HTTPie Desktop  
**Objetivo:** confirmar que um cadastro em cache continua protegido por perfil e CPF.  
**Pre-requisitos:** backend ativo e environment com `baseUrl = http://localhost:3000`.

Este teste complementa CAD-01 e R2. Nao altera a implementacao da API.

## 1. Preparar os dados

Envie `POST {{baseUrl}}/reboot`, sem body e sem token, com timeout de 90 segundos.
Esse comando restaura os dados locais e invalida tokens anteriores.
Espere HTTP 200 com `status: ok`, 5 clientes, 4 gerentes e 5 contas.

## 2. Obter tres tokens

Para cada linha, envie `POST {{baseUrl}}/login`, usando Body -> JSON.
Espere HTTP 200 e salve o campo `token` na variavel indicada, sem aspas.

| Variavel | Body JSON |
|---|---|
| `tokenGerente` | `{"email":"ger1@bantads.com.br","senha":"tads"}` |
| `tokenDona` | `{"email":"cli1@bantads.com.br","senha":"tads"}` |
| `tokenOutroCliente` | `{"email":"cli2@bantads.com.br","senha":"tads"}` |

Nos proximos passos, use `Accept: application/json` e o header
`x-access-token` indicado. Todos os GETs sao enviados sem body.

## 3. Popular o cache com uma consulta autorizada

Envie `GET {{baseUrl}}/clientes/12912861012` com
`x-access-token: {{tokenGerente}}`.

Espere HTTP 200, `cpf: 12912861012`, `nome: Catharyna` e ausencia do campo `saldo`.
Repita a mesma consulta imediatamente: deve continuar retornando HTTP 200.
O Gateway armazena esse cadastro por cinco minutos.

Opcionalmente, confirme a chave no PowerShell, na raiz do DAC:

```powershell
docker compose exec redis redis-cli TTL cache:cliente:12912861012
```

Espere um TTL positivo de ate 300 segundos. Se retornar -2, a chave nao existe;
repita a consulta autorizada antes de prosseguir. Se retornar -1, a chave esta
sem expiracao, contrariando o TTL esperado.

## 4. Tentar acessar o cadastro com outro cliente

Repita `GET {{baseUrl}}/clientes/12912861012`, agora com
`x-access-token: {{tokenOutroCliente}}`.

Espere HTTP 403, mesmo com o cadastro em cache. O corpo nao deve expor
nome, e-mail, endereco ou outros dados do cadastro.

## 5. Consultar como a propria dona

Repita o GET com `x-access-token: {{tokenDona}}`.
Espere HTTP 200 com o CPF `12912861012`.

## 6. Consultar sem autenticacao

Remova completamente o header `x-access-token` e repita o GET.
Espere HTTP 401:

```json
{"auth":false,"message":"Token não fornecido."}
```

## 7. Revogar o token da dona

Envie `POST {{baseUrl}}/logout` com `x-access-token: {{tokenDona}}`, sem body.
Espere HTTP 204 e corpo vazio.

Repita o GET do cadastro com o mesmo token da dona, sem fazer novo login.
Espere HTTP 401 com `auth: false` e a mensagem
`Falha ao autenticar o token.`. O cache nao pode contornar a revogacao.

## Criterios de aceite e registro

| Verificacao | Esperado | Obtido |
|---|---|---|
| Gerente consulta e repete a consulta | 200 / 200 | A executar |
| Outro cliente consulta cadastro em cache | 403, sem dados pessoais | A executar |
| Dona consulta seu cadastro | 200 | A executar |
| Consulta sem token | 401 | A executar |
| Logout da dona | 204, corpo vazio | A executar |
| Token revogado consulta cadastro em cache | 401 | A executar |

Execute os passos 3 a 7 em menos de cinco minutos para manter o cadastro em cache.
Se demorar mais, repita a consulta do gerente e confirme o TTL antes do teste
de token revogado. Nao execute reboot entre os passos, pois ele limpa o cache
e invalida todos os tokens.

Preencha a coluna Obtido somente depois de executar as requisicoes.
Este arquivo descreve resultados esperados; sua criacao nao comprova a execucao.
