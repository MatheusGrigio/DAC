# Guia de teste manual — BANTADS

Teste pela **UI** em `http://localhost:4200` (só fala com o Gateway `http://localhost:3000`). Senha padrão do seed: `tads`. Requisitos HTTP detalhados: `httpie/TX-*.md`.

---

## 1. Preparar o ambiente (faça uma vez)

1. Abra o **Docker Desktop** e espere ficar verde.
2. No PowerShell, na **pasta raiz do projeto**:
   - Se for a primeira vez: `Copy-Item .env.example .env` (mantenha `MAIL_DEV=true` para senha de novo cliente em `outbox/<email>.txt`).
   - Suba tudo: `docker compose up -d --build`.
   - Espere ~1 minuto e confira: `docker compose ps` — linhas **healthy**.
3. Zere o banco para o seed do enunciado:
   ```powershell
    Invoke-RestMethod http://localhost:3000/reboot -Method POST -ContentType application/json -Body "{}"
   ```

    Pode demorar até **90 segundos**. Quando term??inar, deve mostrar `clientes: 5`, `gerentes: 4`, `contas: 5`.
4. Suba o Angular (em outro terminal):
   ```powershell
    cd frontend
    npm install
    npm start
   ```
5. No navegador, abra **http://localhost:4200** — você cai na tela de **login** (rota `/login`).

**Dica:** depois de muitos depósitos, aprovações ou gerentes criados, rode o `reboot` de novo para voltar ao seed conhecido.

### Quem usar nos testes (seed)

| Papel   | E-mail                 | Senha  | Conta (cliente) | Saldo inicial |
|---------|------------------------|--------|-----------------|---------------|
| Cliente | `cli1@bantads.com.br`  | `tads` | `1291`          | R$ 800,00     |
| Cliente 2 (destino) | `cli2@bantads.com.br` | `tads` | `0950` | R$ 10.000,00 |
| Gerente | `ger1@bantads.com.br`  | `tads` | —               | —             |

---

## 2. Lembretes rápidos

- Nos campos de **valor** (depósito/saque/transferência), digite **só números** — os centavos entram sozinhos (ex.: digitar `5000` → R$ 50,00).
- Depósito/saque/transferência podem mostrar “Atualizando o saldo…” — espere; se precisar, volte ao dashboard.
- **Aprovar cliente**, **cadastrar/remover gerente** e **relatório** usam job em segundo plano — o botão pode ficar em “Processando…” alguns segundos.
- **Sair** no dashboard encerra a sessão; para continuar, faça login de novo.

---

## 3. Passo a passo por transação (UI)

Cada bloco tem **caminho feliz (H)** e **caminhos tristes (S)**. Siga na ordem numerada.

---

### R1 — Autocadastro (solicitar conta)

**Onde:** tela pública **Solicitar autocadastro** (`/autocadastro`).

#### H — Enviar solicitação nova

1. Na tela de login, clique em **Solicitar Autocadastro** (ou acesse `http://localhost:4200/autocadastro`).
2. Preencha **Nome completo** (ex.: `Cliente Manual Teste`).
3. **CPF:** 11 dígitos **que ainda não exista** no sistema (ex.: `98765432100`).
4. **E-mail:** um e-mail **novo** (ex.: `novo.manual@teste.local`).
5. **Telefone com DDD:** 11 dígitos (ex.: `41999998888`).
6. **Salário (R$):** ex.: `1500,00`.
7. Em **Endereço**, preencha CEP (8 dígitos), logradouro, número, cidade, UF (ex.: `PR`).
8. Clique **Solicitar cadastro**.
9. **Espere:** mensagem verde de sucesso na própria tela. O cliente **ainda não** consegue logar até o gerente aprovar (R9).

#### S1 — Validação na tela

1. Abra o autocadastro, clique em campos e saia sem preencher, ou coloque CPF com menos de 11 dígitos.
2. **Espere:** textos vermelhos (“Informe o CPF com 11 dígitos”, etc.) e o botão **Solicitar cadastro** desabilitado até corrigir.

#### S2 — CPF ou e-mail duplicado

1. Envie uma solicitação **válida** (H).
2. Tente enviar **outra** com o **mesmo CPF** ou o **mesmo e-mail**.
3. **Espere:** mensagem de erro (conflito) após clicar **Solicitar cadastro**.

---

### R2 — Login e logout

**Onde:** `/login` e botão **Sair** nos dashboards.

#### H1 — Login cliente

1. Abra `http://localhost:4200/login`.
2. No campo **E-mail**, digite `cli1@bantads.com.br`.
3. No campo **Senha**, digite `tads` (use **Mostrar senha** se quiser conferir).
4. Clique **Entrar**.
5. **Espere:** ir para **BANTADS - Área do Cliente** (`/cliente/dashboard`), com seu nome, CPF, e o card **Sua conta** (número e saldo).

#### H2 — Login gerente

1. Se estiver logado, clique **Sair** (canto superior direito no cliente, ou **Sair** no gerente).
2. Na login, e-mail `ger1@bantads.com.br`, senha `tads`, **Entrar**.
3. **Espere:** **BANTADS - Área do Gerente** com botões **Consultar clientes**, **Gerentes**, **Relatório de clientes** e a tabela **Solicitações de Autocadastro**.

#### S1 — Senha errada

1. Na login, e-mail correto e senha `errada`.
2. Clique **Entrar**.
3. **Espere:** alerta vermelho com mensagem de login inválido (não entra no sistema).

#### S2 — E-mail inválido

1. Digite `nao-email` no campo **E-mail** e toque fora.
2. **Espere:** “Informe um e-mail válido.” e botão **Entrar** desabilitado.

#### H3 — Logout

1. Logado, clique **Sair**.
2. **Espere:** voltar à tela de login. Se tentar abrir `/cliente/dashboard` na barra de endereço sem logar, o sistema manda de volta ao login.

---

### R3 — Ver sua conta (cliente)

**Onde:** `/cliente/dashboard`, seção **Sua conta**.

#### H — Consultar conta própria

1. Faça login como **cli1** (R2 H1).
2. No dashboard, espere sair de “Carregando sua conta...”.
3. **Espere:** **Número da conta** `1291` e **Saldo atual** (ex.: R$ 800,00 após reboot).
4. (Opcional) Clique **Tentar novamente** só se aparecer erro vermelho.

#### S — Outra conta (só API)

No navegador o cliente **não** acessa conta alheia; para testar **403**, use HTTPie: `httpie/TX-R3A-consultar-conta-cpf.md`.

---

### R4 — Depósito

**Onde:** dashboard cliente → card **Depósito** → **Acessar** → `/cliente/deposito`.

#### H — Depositar valor válido

1. Login como **cli1**.
2. No dashboard, no card **Depósito**, clique **Acessar**.
3. Confira **Conta** e **Saldo atual** no topo.
4. No campo **Valor do depósito**, digite só números, ex.: `5000` para R$ 50,00.
5. Clique **Depositar**.
6. **Espere:** mensagem de sucesso e “Atualizando seu saldo…”, depois saldo maior. Use **Voltar** para o dashboard e confira o card **Sua conta**.

#### S — Gerente não deposita (API)

Gerente **não** tem tela de depósito. Teste **403** via `httpie/TX-R4-deposito.md`.

---

### R5 — Saque

**Onde:** dashboard → **Saque** → **Acessar** → `/cliente/saque`.

#### H — Saque dentro do saldo

1. Login **cli1**.
2. Abra **Saque**.
3. Em **Valor do saque**, digite ex.: `1000` (R$ 10,00).
4. Clique **Sacar**.
5. **Espere:** alerta verde de sucesso e saldo atualizado após “Atualizando o saldo...”.

#### S — Saldo insuficiente

1. Na mesma tela, digite um valor **maior** que o saldo (ex.: saldo ~800 → tente `100000` = R$ 1.000,00).
2. Clique **Sacar**.
3. **Espere:** alerta vermelho com mensagem de saldo insuficiente (ou botão desabilitado se a validação for antecipada).

---

### R6 — Transferência

**Onde:** dashboard → **Transferência** → **Acessar** → `/cliente/transferencia`.

#### H — Transferir para outra conta

1. Login **cli1** (conta origem `1291`).
2. Abra **Transferência**.
3. Em **Conta de destino (4 dígitos)**, digite `0950` (conta do Catharyna/cli2).
4. Em **Valor da transferência**, digite ex.: `2500` (R$ 25,00).
5. Clique **Transferir**.
6. **Espere:** sucesso e saldo menor na origem; login como cli2 mostra saldo maior (opcional).

#### S1 — Mesma conta

1. Em **Conta de destino**, digite o **mesmo número** da sua conta (`1291`).
2. **Espere:** texto vermelho “A conta de destino não pode ser a mesma da origem.” e botão **Transferir** desabilitado.

#### S2 — Conta inexistente

1. Digite destino `0000`, valor válido, **Transferir**.
2. **Espere:** mensagem de erro (conta não encontrada ou regra de negócio).

---

### R7 — Extrato e dados cadastrais

#### H — Extrato

1. Login **cli1** → card **Extrato** → **Acessar** (`/cliente/extrato`).
2. Ajuste **De** e **Até** (datas; não podem passar de hoje).
3. Clique **Consultar**.
4. **Espere:** tabela com movimentações (depósitos/saques/transferências que você fez) e saldos por dia.

#### S — Intervalo inválido (API)

Na UI as datas limitam o calendário; intervalo > 365 dias — teste via `httpie/TX-R7-extrato.md`.

#### H — Dados cadastrais

1. No dashboard cliente, card **Dados cadastrais** → **Acessar** (`/cliente/dados`).
2. **Espere:** nome **Catharyna**, CPF, e-mail, telefone, salário e endereço completos. **Voltar** ao dashboard.

---

### R8 — Listar solicitações (gerente)

**Onde:** `/gerente/dashboard`, seção **Solicitações de Autocadastro**.

#### H1 — Ver pendentes após autocadastro

1. Faça antes o **R1 H** (CPF novo).
2. Login **ger1** → área do gerente.
3. Role até **Solicitações de Autocadastro**. Se a tabela estiver vazia, clique **Atualizar lista**.
4. **Espere:** uma linha com o CPF/nome do cadastro, badge **Pendente**, botões **Aprovar**, **Recusar**, **Detalhes**.

#### H2 — Detalhe da solicitação

1. Na mesma linha, clique **Detalhes**.
2. **Espere:** abaixo da linha, texto com **Endereço:** logradouro, cidade/UF, CEP.

#### S — Cliente não vê lista (API)

Cliente não tem essa tela; `GET /solicitacoes` como cliente → **403** em HTTPie (`TX-R8A`).

---

### R9 — Aprovar cliente (SAGA)

**Onde:** mesma tabela do gerente, botão **Aprovar**.

#### H — Aprovar e logar com senha do e-mail

1. Garanta solicitação **Pendente** (R1 + R8).
2. Na linha do candidato, clique **Aprovar**.
3. No diálogo do navegador **“Deseja realmente aprovar este cliente?”**, clique **OK**.
4. **Espere:** botão vira **Processando...**, depois alerta verde **“Cliente aprovado com sucesso!”** e a linha some ou muda para **Aprovado**.
5. No explorador de arquivos do projeto, abra `outbox/` e o arquivo do e-mail usado no R1 (ex.: `novo.manual@teste.local.txt`). Leia a linha `senha: ...`.
6. Clique **Sair**, vá ao login, use o **e-mail do cadastro** e a **senha do arquivo**.
7. **Espere:** dashboard **cliente** (primeiro login do novo usuário).

#### S — Aprovar de novo / e-mail de gerente

- Repetir **Aprovar** no mesmo CPF já aprovado: erro ou job falha (teste avançado via API `TX-R9`).
- Cadastro com e-mail já usado por gerente: aprovação falha no job (API).

---

### R10 — Rejeitar solicitação

**Onde:** botão **Recusar** na tabela do gerente.

#### H — Rejeitar com motivo

1. Faça **outro** autocadastro (R1) com **outro CPF** (para não misturar com o aprovado no R9).
2. Login gerente → **Atualizar lista**.
3. Na linha **Pendente**, clique **Recusar**.
4. No prompt **“Informe o motivo da rejeição:”**, digite ex.: `Documentação incompleta` e confirme (**OK**).
5. **Espere:** mensagem **“Cliente rejeitado com sucesso.”** e status **Não aprovado** / motivo na coluna.

#### S1 — Cancelar motivo

1. Em outra linha pendente, clique **Recusar**.
2. No prompt, clique **Cancelar** ou feche sem texto.
3. **Espere:** nada muda; linha continua **Pendente**.

#### S2 — Motivo vazio (API)

Enviar rejeição sem motivo → **400** (`TX-R10`).

---

### R11 — Buscar clientes

**Onde:** gerente → **Consultar clientes** → `/gerente/clientes`.

#### H — Busca com resultado

1. Login **ger1** → **Consultar clientes**.
2. No campo **Nome ou CPF**, digite `Cat` (encontra Catharyna, Catianna, etc.).
3. Clique **Buscar**.
4. **Espere:** tabela com CPF formatado, nome, cidade **Curitiba**, estado **PR**, saldo.

#### S — Busca vazia

1. Digite `zzz` e **Buscar**.
2. **Espere:** uma linha **“Nenhum cliente encontrado.”**

---

### R12 — Listar gerentes

**Onde:** gerente → **Gerentes** → `/gerente/gerentes`.

#### H — Ver os 4 do seed

1. Login **ger1** → **Gerentes**.
2. **Espere:** tabela com **4** linhas (Geniéve, Godophredo, Gyândula, Gadamântio), colunas e-mail, telefone, **Quantidade de Clientes**, ações **Editar** / **Remover** (quando permitido).

#### S — Cliente não acessa (API)

Cliente não tem essa rota; teste `GET /gerentes` com token cliente → **403**.

---

### R13 — Inserir gerente (SAGA)

**Onde:** mesma tela **Gerentes**.

#### H — Cadastrar gerente novo

1. Em **Gerentes**, clique **Novo gerente**.
2. Preencha **Nome completo**, **CPF** novo (11 dígitos), **E-mail** novo, **Telefone**, **Senha**.
3. Clique **Cadastrar gerente**.
4. **Espere:** processamento breve, mensagem de sucesso, formulário some, **nova linha** na tabela.

#### S — E-mail duplicado

1. Tente cadastrar outro gerente com o **mesmo e-mail** de um existente (ex.: `ger1@bantads.com.br`).
2. **Espere:** erro / job em falha após enviar (pode aparecer no alerta vermelho).

---

### R14 — Atualizar gerente

**Onde:** **Gerentes** → **Editar** em outra linha (não precisa ser você).

#### H — Mudar nome e telefone

1. Na lista, clique **Editar** em um gerente (ex.: **Gadamântio**).
2. Altere **Nome completo** e **Telefone com DDD**.
3. Clique **Salvar alterações**.
4. **Espere:** voltar à lista com dados atualizados.

#### S — E-mail imutável (API)

A UI **não** edita e-mail; tentativa via API → **422** (`TX-R14`).

---

### R15 — Remover gerente (SAGA)

**Onde:** **Gerentes** → **Remover**.

#### H — Remover outro gerente

1. Remova preferencialmente o gerente criado no **R13** (ou `ger4@bantads.com.br` se ainda tiver link **Remover**).
2. Clique **Remover** → confirme **OK** no diálogo.
3. **Espere:** **Removendo...**, depois some da lista ou fica inativo.
4. (Opcional) tente login com aquele e-mail → falha.

#### S1 — Não remover a si mesmo

1. Na linha do **gerente logado** (Geniéve / ger1), **não** deve aparecer **Remover** (sem link HATEOAS).

#### S2 — Login após remoção

1. Após remover um gerente, faça login com o e-mail dele.
2. **Espere:** login inválido / inativo.

---

### R16 — Relatório de clientes

**Onde:** **Relatório de clientes** → `/gerente/relatorio`.

#### H — Gerar relatório

1. Login **ger1** → **Relatório de clientes**.
2. Na primeira vez a página já carrega; senão clique **Atualizar relatório**.
3. **Espere:** tabela ampla (CPF, nome, e-mail, salário, **número da conta**, **saldo**, CPF/nome do gerente). Com seed puro, **5** clientes; depois de aprovar R9, **6**.

#### S — Cliente não acessa (API)

Cliente não tem menu de relatório; `GET /relatorios/clientes` com token cliente → **403**.

---

## 4. Roteiro completo na UI (um passe sugerido)

Faça do início ao fim como se fosse a demonstração do DS:

1. **Reboot** no PowerShell (seção 1).
2. **R1** — autocadastro com CPF/e-mail novos (anote os dados).
3. **R2 H2** — login gerente → **R8** conferir pendente → **R10** rejeite um segundo cadastro *ou* **R9** aprove o primeiro (e teste login com `outbox/`).
4. **R2** logout → **R2 H1** cliente **cli1** → **R3–R7** (depósito, saque, transferência `0950`, extrato, dados cadastrais).
5. **R2** logout → gerente → **R11** busca → **R13** novo gerente → **R14** editar → **R15** remover o criado → **R16** relatório.
6. **Sair** e encerrar.

Ordem equivalente na API: `httpie/00-GENERAL-INFO.md`.

---

## 5. Sanidade rápida (API / HTTPie)

| Check | Como | OK |
|-------|------|-----|
| Gateway no ar | `GET http://localhost:3000/health` | `{ "status": "UP" }` |
| Seed | `POST http://localhost:3000/reboot` | 5 clientes, 4 gerentes, 5 contas |

---

## 6. Parar

```powershell
docker compose down
```

`reboot` restaura o seed sem apagar volumes. Banco do zero: `docker compose down -v`.

---

## 7. Automatizar tudo

```powershell
.\run-test-campanha.ps1
```

Relatório consolidado: `RELATORIO-TESTES-COMPLETO.md`.