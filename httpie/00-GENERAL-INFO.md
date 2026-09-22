# BANTADS — Como subir o projeto e testar no HTTPie Desktop

**Arquivo:** `00-GENERAL-INFO`  
**Base URL do Gateway:** `http://localhost:3000`  
**Front e testes falam somente com o Gateway.** Os microsserviços (portas 808x) não são públicos.

Este arquivo é o ponto de partida. Depois de o sistema estar no ar, use um arquivo `TX-*.md` desta pasta para cada transação (cada uma corresponde a um diagrama de sequência do sistema).

---

## 1. O que precisa estar instalado

| Ferramenta | Para quê |
|---|---|
| [Docker Desktop](https://www.docker.com/products/docker-desktop/) | Sobe Gateway, MSs, Postgres, MongoDB, Redis e RabbitMQ |
| Git Bash **ou** WSL | O script `start.sh` da raiz é Bash |
| PowerShell 7+ | Compilação sequencial (`compile-services.ps1`) no Windows |
| [HTTPie Desktop](https://httpie.io/download) | Cliente HTTP gráfico para as transações |
| Java 21 e Node 22 (opcional) | Só se for compilar fora do Docker |

RAM: o compose foi dimensionado para cerca de **8 GB**. Feche outros Docker pesados antes de subir.

---

## 2. Preparar o ambiente (uma vez)

Na raiz do repositório (`C:\Users\zuria\CODE\DAC` ou o clone local):

1. Confirme que o Docker Desktop está **Running**.
2. Copie o arquivo de exemplo de variáveis (não commite o `.env`):

```powershell
Copy-Item .env.example .env
```

3. O `.env.example` já traz valores locais (`change-me`, `MAIL_DEV=true`). Para testar no HTTPie **não precisa** de Gmail: com `MAIL_DEV=true` a senha gerada na aprovação de cliente (R9) é gravada em `outbox/<email>.txt`.

---

## 3. Compilar a frota (recomendado antes do primeiro build Docker)

No Windows, **nunca** rode Gradle/npm/Compose em paralelo na frota. Use o script da raiz:

```powershell
.\compile-services.ps1
```

Ordem: `shared` → `auth` → `cliente` → `gerente` → `conta` → `saga` → `email` → `gateway` → `frontend` (se existir).

O `docker compose build` também compila dentro das imagens (multi-stage). Compilar antes só acelera e detecta erro de código mais cedo.

---

## 4. Subir os contêineres

### Opção A — Git Bash / WSL (oficial)

```bash
./start.sh
```

O script constrói **um serviço por vez** (`docker compose build <svc>`) e depois `docker compose up -d`. A primeira vez demora vários minutos.

### Opção B — PowerShell (equivalente)

```powershell
docker compose config --services | ForEach-Object {
  Write-Host "==> docker compose build $_"
  docker compose build $_
  if ($LASTEXITCODE -ne 0) { throw "build falhou: $_" }
}
docker compose up -d
```

### Conferir se subiu

```powershell
docker compose ps
```

Todos os serviços devem ficar `healthy` (não só `started`). O Gateway publica **3000** no host. Postgres 5432, Mongo 27017, Redis 6379, RabbitMQ 5672 e management **15672** também saem no host; as portas 808x dos MSs **não**.

Health do Gateway (pode testar no navegador ou no HTTPie):

```
GET http://localhost:3000/health
```

Resposta esperada: HTTP **200**

```json
{ "status": "UP" }
```

Se o Gateway ainda não responder, espere o `start_period` dos MSs Java (~40 s cada) e tente de novo. `POST /reboot` (próximo passo) pode levar até ~90 s na primeira chamada.

---

## 5. Recriar o seed (obrigatório antes dos tutoriais)

O estado conhecido do enunciado (5 clientes, 4 gerentes, 5 contas) é recriado por:

```
POST http://localhost:3000/reboot
```

Resposta esperada: HTTP **200**, **sem** `_links`

```json
{
  "status": "ok",
  "clientes": 5,
  "gerentes": 4,
  "contas": 5
}
```

No HTTPie Desktop, aumente o **timeout do request para 90 segundos** neste POST (padrão costuma ser 30 s e o reboot falha por timeout mesmo tendo dado certo).

Faça `reboot` de novo sempre que um tutorial avisar “estado sujo” (depois de depósito, aprovação, inserção de gerente, etc.). Os tutoriais assumem seed fresco, salvo quando o próprio arquivo pede um passo anterior.

Detalhes: [TX-INFRA-02-reboot.md](./TX-INFRA-02-reboot.md).

---

## 6. Configurar o HTTPie Desktop (uma vez)

1. Abra o **HTTPie Desktop**.
2. Crie um **Environment** chamado `BANTADS Local` com estas variáveis:

| Nome | Valor inicial |
|---|---|
| `baseUrl` | `http://localhost:3000` |
| `tokenCliente` | *(vazio — preencher após o login R2)* |
| `tokenGerente` | *(vazio — preencher após o login R2)* |
| `jobId` | *(vazio — preencher após um 202)* |
| `cpfNovo` | `11122233396` |

3. Selecione esse environment no seletor do topo.
4. Crie uma **Collection** `BANTADS`.
5. Em **cada request** desta collection:
   - URL no formato `{{baseUrl}}/caminho`
   - Header `Accept`: `application/json`
   - Header de autenticação (rotas protegidas): **`x-access-token`** = `{{tokenCliente}}` ou `{{tokenGerente}}`
   - **Não** use `Authorization: Bearer`. O contrato é o header `x-access-token`.
   - Body: aba **Body → JSON** (o HTTPie envia `Content-Type: application/json` sozinho).
6. Depois de um login bem-sucedido, copie o campo `token` da resposta e cole na variável `tokenCliente` ou `tokenGerente` do environment. Requests seguintes passam a autenticar.
7. Timeout: **90 s** em `/reboot`; **15–30 s** no restante. Operações 202 (SAGA/relatório) respondem na hora; o tempo longo está no **polling** do job (até ~45 s).

### Como “Send” e ler a resposta

1. Escolha o método (GET/POST/PUT/DELETE).
2. Cole a URL.
3. Preencha Headers e, se houver, o JSON.
4. Clique em **Send**.
5. Confira **status HTTP**, **headers** (`Location` nos 201/202) e o **JSON** do corpo.

---

## 7. Convenções que valem para todas as transações

- **Dinheiro:** string com ponto e 2 casas (`"800.00"`). Nunca number JSON.
- **CPF:** 11 dígitos, sem pontuação (`"12912861012"`).
- **Conta:** 4 dígitos string, pode ter zero à esquerda (`"0950"`).
- **Datas no JSON:** ISO 8601 **sem offset**, fuso `America/Sao_Paulo` (`2026-04-30T10:00:00`).
- **Datas em query:** `YYYY-MM-DD`.
- **HATEOAS:** DTOs de negócio trazem `_links`. **Não** há `_links` em login, jobs (202/status/result), `/health` e `/reboot`.
- **Erros 401 de auth:** `{ "auth": false, "message": "..." }` (não usam `status/erro/mensagem`).
- **Demais erros:** `{ "status": 422, "erro": "Unprocessable Entity", "mensagem": "..." }`.
- **SAGA / relatório:** o POST/GET inicial devolve **202** + job `PENDENTE`. Falha de negócio **não** vem como 4xx desse POST: aparece depois no job `FALHA`.
- **CQRS (conta):** depósito/saque/transferência **não** devolvem o novo saldo. Reconsulte a conta; se o saldo ainda for o antigo, espere 2 s e tente de novo (projeção assíncrona).

---

## 8. Usuários e contas do seed (senha de todos: `tads`)

### Clientes

| Nome | CPF | E-mail | Conta | Saldo seed | Gerente | Salário |
|---|---|---|---|---|---|---|
| Catharyna | `12912861012` | `cli1@bantads.com.br` | `1291` | `"800.00"` | Geniéve | `"10000.00"` |
| Cleuddônio | `09506382000` | `cli2@bantads.com.br` | `0950` | `"10000.00"` | Godophredo | `"20000.00"` |
| Catianna | `85733854057` | `cli3@bantads.com.br` | `8573` | `"200.00"` | Gyândula | `"3000.00"` |
| Cutardo | `58872160006` | `cli4@bantads.com.br` | `5887` | `"150000.00"` | Geniéve | `"500.00"` |
| Coândrya | `76179646090` | `cli5@bantads.com.br` | `7617` | `"1500.00"` | Godophredo | `"1500.00"` |

Endereços (todos Curitiba/PR): Catharyna — Rua XV de Novembro 1299 CEP `80060000`; Cleuddônio — Rua Marechal Deodoro 630 CEP `80010010`; Catianna — Avenida Sete de Setembro 2775 CEP `80230010`; Cutardo — Rua Comendador Araujo 143 CEP `80420000`; Coândrya — Rua Emiliano Perneta 390 CEP `80420080`. Telefones `41999990001` … `41999990005`.

### Gerentes (todos ativos; senha `tads`)

| Nome | CPF | E-mail | Telefone | Qtde. clientes no seed |
|---|---|---|---|---|
| Geniéve | `98574307084` | `ger1@bantads.com.br` | `41988880001` | 2 |
| Godophredo | `64065268052` | `ger2@bantads.com.br` | `41988880002` | 2 |
| Gyândula | `23862179060` | `ger3@bantads.com.br` | `41988880003` | 1 |
| Gadamântio | `40501740066` | `ger4@bantads.com.br` | `41988880004` | 0 |

Login de teste mais usado: **cliente** `cli1@bantads.com.br` / **gerente** `ger1@bantads.com.br`.

---

## 9. Catálogo das transações (diagramas de sequência)

Cada arquivo é uma transação ponta a ponta. IDs são estáveis (letras + números) para citar em diagramas.

| ID | Arquivo | Tipo | Perfil |
|---|---|---|---|
| `TX-INFRA-01` | [TX-INFRA-01-health.md](./TX-INFRA-01-health.md) | Sync | Público |
| `TX-INFRA-02` | [TX-INFRA-02-reboot.md](./TX-INFRA-02-reboot.md) | Sync (seed) | Público |
| `TX-R2A` | [TX-R2A-login.md](./TX-R2A-login.md) | Sync | Público |
| `TX-R2B` | [TX-R2B-logout.md](./TX-R2B-logout.md) | Sync | CLIENTE ou GERENTE |
| `TX-R1` | [TX-R1-autocadastro.md](./TX-R1-autocadastro.md) | Sync | Público |
| `TX-R3A` | [TX-R3A-consultar-conta-cpf.md](./TX-R3A-consultar-conta-cpf.md) | Query CQRS | CLIENTE dono ou GERENTE |
| `TX-R3B` | [TX-R3B-consultar-conta-numero.md](./TX-R3B-consultar-conta-numero.md) | Query CQRS | CLIENTE dono ou GERENTE |
| `TX-R4` | [TX-R4-deposito.md](./TX-R4-deposito.md) | Command + ES | CLIENTE dono |
| `TX-R5` | [TX-R5-saque.md](./TX-R5-saque.md) | Command + ES | CLIENTE dono |
| `TX-R6` | [TX-R6-transferencia.md](./TX-R6-transferencia.md) | Command atômico + enrich | CLIENTE dono |
| `TX-R7` | [TX-R7-extrato.md](./TX-R7-extrato.md) | Query CQRS | CLIENTE dono |
| `TX-R8A` | [TX-R8A-listar-solicitacoes.md](./TX-R8A-listar-solicitacoes.md) | Query | GERENTE |
| `TX-R8B` | [TX-R8B-consultar-solicitacao.md](./TX-R8B-consultar-solicitacao.md) | Query | GERENTE |
| `TX-R9` | [TX-R9-aprovar-cliente.md](./TX-R9-aprovar-cliente.md) | **SAGA** 202 | GERENTE |
| `TX-R10` | [TX-R10-rejeitar-cliente.md](./TX-R10-rejeitar-cliente.md) | Sync + e-mail FF | GERENTE |
| `TX-CAD-01` | [TX-CAD-01-consultar-cliente.md](./TX-CAD-01-consultar-cliente.md) | Query + cache | GERENTE ou próprio CLIENTE |
| `TX-R11` | [TX-R11-consultar-clientes.md](./TX-R11-consultar-clientes.md) | API Composition | GERENTE |
| `TX-R12` | [TX-R12-listar-gerentes.md](./TX-R12-listar-gerentes.md) | API Composition | GERENTE |
| `TX-CAD-02` | [TX-CAD-02-consultar-gerente.md](./TX-CAD-02-consultar-gerente.md) | Query + cache | GERENTE |
| `TX-R13` | [TX-R13-inserir-gerente.md](./TX-R13-inserir-gerente.md) | **SAGA** 202 | GERENTE |
| `TX-R14` | [TX-R14-atualizar-gerente.md](./TX-R14-atualizar-gerente.md) | Sync | GERENTE |
| `TX-R15` | [TX-R15-remover-gerente.md](./TX-R15-remover-gerente.md) | **SAGA** 202 | GERENTE |
| `TX-R16` | [TX-R16-relatorio-clientes.md](./TX-R16-relatorio-clientes.md) | Composition async 202 | GERENTE |
| `TX-JOB-01` | [TX-JOB-01-status.md](./TX-JOB-01-status.md) | Polling Redis | dono do job |
| `TX-JOB-02` | [TX-JOB-02-result.md](./TX-JOB-02-result.md) | Resultado inline | dono do job |

### Ordem sugerida no HTTPie

1. `TX-INFRA-01` → `TX-INFRA-02`  
2. `TX-R2A` (cliente e gerente; grave os tokens)  
3. Leituras: `TX-R3A`, `TX-R3B`, `TX-CAD-01`, `TX-R7`, `TX-R8A`, `TX-R11`, `TX-R12`, `TX-CAD-02`  
4. Escritas síncronas: `TX-R4` → `TX-R5` → `TX-R6` (reboot entre blocos se quiser saldos do seed)  
5. Autocadastro: `TX-R1` → `TX-R8B` → `TX-R10` **ou** `TX-R9` (não os dois no mesmo CPF)  
6. CRUD gerente: `TX-R14` → `TX-R13` → `TX-R15`  
7. `TX-R16` + `TX-JOB-01` / `TX-JOB-02`  
8. `TX-R2B` por último (invalida o token)

---

## 10. Parar o ambiente

```powershell
docker compose down
```

Volumes (`postgres_data`, `mongo_data`, …) persistem. O `POST /reboot` já reconstrói o seed sem precisar apagar volumes. Para zero absoluto: `docker compose down -v` (apaga dados).
