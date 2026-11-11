# Relatório de rechecagem do backend — para correção

Audiência: o próximo agente que for **corrigir** o backend. Não é um resumo para defesa.
Data da auditoria estática: 2026-10-27. A suíte **não foi executada** (`pytest`, Gradle, `docker compose`). Cada item abaixo foi lido no código e confrontado com `docs/swagger_bantads.md` (vence), `docs/bantads.md` e, em seguida, `prompts/backend_plan.md`.

Antes de editar, leia o agente em `.cursor/agents/` do domínio tocado e o trecho do Swagger/enunciado citado no item. Não refatore o que a seção **Não mexer** marca como conforme.

## Como usar este arquivo

1. Corrija na ordem da seção **Ordem de correção**. Itens posteriores dependem da topologia Rabbit e da idempotência.
2. Um item = um comportamento observável. Não “aproveite” para redesenhar SAGA, CQRS ou HATEOAS.
3. Ao terminar um bloco, rode o teste indicado nele. No fim, a seção **Verificação**.
4. `definitions.json` novo **não altera** um volume `rabbitmq_data` já inicializado. Depois de mudar filas: `docker compose down` e apagar o volume do Rabbit (ou as filas no management) antes de subir de novo. Fila existente com `arguments` diferentes sobe `PRECONDITION_FAILED`.

## Estado geral

R1–R16, as três SAGAs (passos, compensação, e-mail fire-and-forget, caso e-mail duplicado, logout forçado R15), CQRS com event store separado do read model, tipos de evento com acento, replay de saldo, transferência atômica, seed/reboot, JWT+Redis, HATEOAS com rewrite e dinheiro como string estão implementados e **não devem ser reescritos**.

O que impede a stack Docker de cumprir o enunciado:

- o broker não declara as filas que os listeners consomem;
- não há retentativa 3× / 5s nem caminho real até a DLQ;
- o MS Email tem dois consumidores na mesma fila;
- reentrega de comando vira `FALHA` de negócio (e, no Auth, grava a senha em claro no Mongo).

---

## Não mexer

Estas leituras de auditoria automática estavam erradas ou o efeito já está correto. Não implemente “correção” aqui.

| Ideia tentadora | Por que não |
|---|---|
| Invalidar `cache:cliente` / `cache:gerente` no Gateway em R9/R13/R15 | O efeito já ocorre no sucesso da SAGA: `SagaEngine.complete` chama `RedisCacheInvalidator` (`backend/services/saga/.../SagaEngine.kt` por volta das linhas 421, 434, 451). Redis é compartilhado. Apagar no `202` seria cedo demais (a escrita ainda não ocorreu). R14 já apaga no Gateway (`proxy.ts` ~229). |
| Travar `GET /contas/{numero}` no ACL do Gateway | `selfCpfFromPath` não lê número de conta (`acl.ts` 67–91), então o Gateway deixa o CLIENTE passar. Quem aplica a posse é o MS Conta: `ContaQueryService.obterPorNumero` → `Identity.requireGerenteOrOwner` (`ContaQueryService.kt` 23–30). O HTTP final é 403. Não duplique a checagem. |
| Trocar valor `<= 0` de 422 para 400 | `ContaCommandService.requirePositive` devolve 422. O plano pede 400; o Swagger manda: 400 = malformado, 422 = regra de negócio. `"0.00"` casa com `^\d+\.\d{2}$`, logo não é malformado. **Mantenha 422.** |
| Remover o passo `gerente.listar-ativos` da SAGA R13 | `SagaRegistry.inserirGerente` linhas 110–114. O MS Conta não pode ler o banco do Gerente. A lista de CPFs ativos precisa chegar no payload de `conta.identificar-conta-para-novo-gerente`. O passo extra é necessário. |
| Apagar o tipo de evento `Removido` | Tombstone interno da compensação R9 (`EventTypes` / `removerSeSoCriado`). Não entra no extrato. O plano aceita. Não vira tipo público do enunciado. |
| Esperar reply das compensações antes de marcar o job `FALHA` | Com fila durável, a compensação fica no broker até o MS voltar. Não construa uma submáquina “COMPENSANDO” nesta rodada. |
| Criar proxy HTTP para `SAGA_URL` / `EMAIL_URL` | Variáveis mortas no `docker-compose.yml` do Gateway (~144–145). SAGA e e-mail são só AMQP. Pode remover as envs; não crie cliente HTTP. |
| Reescrever ordenação, HATEOAS, login, reboot, seed | Tratados como alinhados nesta leitura. |

---

## Ordem de correção

### 1. BLOQUEANTE — Topologia RabbitMQ inexistente

**Requisito:** `docs/bantads.md` §5.7 e `prompts/backend_plan.md` FASE 1 (retry: falha → fila wait TTL 5s → volta à principal; após 3 `x-death` → DLQ). DLQ de `*.cmd` é consumida pelo orquestrador. DLQ de `ms.conta.events` **não** é reenfileirada.

**Atual:** `db/rabbitmq/definitions.json` só declara `ms.auth.cmd`, `ms.auth.cmd.dlq`, `ms.auth.exchange` e `ms.auth.dlx`. O binding de `ms.auth.cmd` manda a DLX **direto** para a DLQ, sem fila wait.

Nenhum `@Bean Queue` de produção existe fora de `backend/services/email/.../RabbitMQConfig.kt` (`ms.email.cmd` durável, sem arguments). `@RabbitListener(queues = "...")` **não** cria fila. Com `missingQueuesFatal` default true, sobem só o que já existe:

| Fila | Quem precisa | Existe no broker fresco? |
|---|---|---|
| `saga.cmd` | Gateway publica; SAGA consome | Só depois que o Gateway faz `assertQueue` em `backend/gateway/src/amqp/publisher.ts` 20 (durável, sem DLX) |
| `orquestrador.reply` | MSs publicam; SAGA consome | Não |
| `ms.cliente.cmd`, `ms.conta.cmd`, `ms.gerente.cmd` | SAGA → MS | Não |
| `ms.conta.events` | command → query | Não |
| `ms.*.cmd.dlq`, `*.wait`, `ms.conta.events.dlq` | retry / `DlqListener` | Não, salvo `ms.auth.cmd.dlq` |
| `ms.email.cmd` | e-mail | Só quando o MS Email sobe e o `RabbitAdmin` declara o bean |

Os ITs passam porque `backend/services/saga/src/test/.../TestAmqpQueues.kt` declara fila no teste. Isso não vale no compose.

**Listeners não rejeitam para a DLQ.** Exemplo: `AuthCommandListener.onCommand` não tem `try/catch`. Exceção → Spring requeue `true` → a mensagem **volta para a mesma fila para sempre** e nunca passa pela DLX. `DlqListener` (`backend/services/saga/.../amqp/DlqListener.kt`) escuta as quatro `*.cmd.dlq` e chama `SagaEngine.onDlq`, mas nada entrega mensagem lá. Não há `defaultRequeueRejected`, `RetryTemplate` nem contagem de `x-death` em lugar nenhum.

**Correção:**

1. Reescreva `db/rabbitmq/definitions.json` como fonte única. Default exchange para publicação (o código faz `convertAndSend(nomeDaFila, body)` e `sendToQueue`). Uma DLX direct, por exemplo `bantads.dlx`.
2. Filas duráveis, sem TTL e sem DLX: `saga.cmd`, `orquestrador.reply`, `ms.email.cmd`. O `assertQueue` do Gateway em `saga.cmd` tem de continuar **idêntico** (durable, sem arguments), senão o Gateway quebra ao subir.
3. Para cada uma de `ms.cliente.cmd`, `ms.conta.cmd`, `ms.gerente.cmd`, `ms.auth.cmd`, `ms.conta.events`:
   - principal: `x-dead-letter-exchange=bantads.dlx`, `x-dead-letter-routing-key=<fila>.wait`;
   - `<fila>.wait`: `x-message-ttl=5000`, dead-letter de volta à principal (exchange default, routing key = nome da principal);
   - `<fila>.dlq`: durável, sem TTL;
   - bindings da DLX: `*.wait` e `*.dlq`.
4. Substitua a declaração atual de `ms.auth.cmd` (hoje a DLX aponta para a DLQ). Isso exige volume Rabbit novo.
5. Nos listeners de comando (auth, cliente, gerente, conta) e no `ContaEventListener`: `defaultRequeueRejected=false`. Num error handler compartilhado (módulo `shared` ou config de cada MS): se o header `x-death` dessa fila já contabiliza 3 mortes, publique na `<fila>.dlq` e dê ack; senão `basicNack(requeue=false)` para cair na wait. Não engula a exceção com ack.
6. `DlqListener` continua **só** nas quatro `*.cmd.dlq`. Não registre `ms.conta.events.dlq` nele. Reprocessamento dessa DLQ é manual, pelo console, na defesa.
7. Apague o `@Bean emailQueue()` **ou** deixe-o rigorosamente igual à fila do JSON (durable, sem arguments). Duas declarações com args diferentes derrubam o MS Email.
8. Nomes canônicos já estão em `backend/services/shared/.../amqp/QueueNames.kt`. Use esses, sem inventar outros.

**Teste:** com volume Rabbit limpo, `docker compose up`. No management (`:15672`) as filas da tabela existem, inclusive `.wait` e `.dlq`. Os seis MS Java e o Gateway ficam healthy. Um comando inválido em `ms.auth.cmd` aparece na DLQ depois de 3 voltas (~15s) e o `DlqListener` loga `cmd.dlq`.

### 2. BLOQUEANTE — Dois consumidores em `ms.email.cmd`

**Requisito:** §5.2 / §5.9. Um consumidor. Fire-and-forget. Tipos `email.senha-cliente`, `email.falha-aprovacao`, `email.rejeicao`, `email.troca-gerente`. Sem reply em `orquestrador.reply`. Sem logar senha.

**Atual:** dois `@RabbitListener` na mesma fila:

- `backend/services/email/.../amqp/EmailCommandListener.kt` — caminho certo, delega a `EmailCommandService`.
- `backend/services/email/.../amqp/EmailListener.kt` — stub. Lê `payload.email` / `payload.mensagem` / `payload.assunto`, manda SMTP genérico e **dá ack mesmo no catch**. O envelope real não tem esse formato (`EmailCommandService` + `Destinatarios` / `EmailComposer`).

Consumidores competem: cerca de metade das mensagens (senha da R9, rejeição R10, troca de gerente R13/R15) é consumida pelo stub e perdida.

**Correção:** apague `EmailListener.kt` e o bean só usado por ele. Fique apenas `EmailCommandListener`. Não faça o stub “entender” o envelope. Confirme que nenhum `log` imprime `payload` nem senha (`EmailCommandListener` linha 23 hoje só loga tipo e sagaId — mantenha assim).

**Teste:** `EmailIT` existente. Publicar uma mensagem de cada tipo e ver um único envio no outbox (`MAIL_DEV`), não dois e não zero.

### 3. ALTO — Idempotência da SAGA e senha em claro no Mongo

**Requisito:** §5.7 consumidores idempotentes em `(sagaId, tipo)`. §5.9: a senha em claro da R9 só existe na reply AMQP do Auth e no comando `email.senha-cliente`. Não vai para Redis, log nem banco. O caso “E-mail já cadastrado” marca a solicitação como não aprovada **só** quando o e-mail pertence a **outra** pessoa.

**Atual:** o mesmo desenho nos quatro handlers (`AuthCommandHandler.handle` 21–28, `ClienteCommandHandler` 24–30, `GerenteCommandHandler`, `ContaCommandHandler` 20–27):

1. lê o inbox;
2. executa o efeito;
3. só então grava o inbox.

Janela: crash ou requeue entre 2 e 3 reexecuta o efeito.

Pior caso, Auth (`AuthService.criarCliente` 30–35 e `persist` 99–100): o usuário já foi salvo; a reentrega acha o login e devolve `Falha("E-mail já cadastrado")`. `SagaEngine.isEmailDuplicado` (constante linha 498) trata **qualquer** `FALHA` de `auth.criar-cliente` com esse texto como o caso especial e troca a compensação de `cliente.marcar-aprovada` por `cliente.marcar-nao-aprovada`. Uma reentrega vira reprovação permanente de uma solicitação que tinha acabado de criar o usuário.

O inbox do Auth grava o `ReplyEnvelope` inteiro, inclusive `payload.senha` (`AuthCommandHandler` 65–67 → `inbox.save`). Isso é Mongo, proibido pelo enunciado. `SagaSecrets` / `capturePassword` (`SagaEngine` 478–488) já guardam a senha **só em memória** a partir da reply AMQP. O Redis da SAGA passa por `withoutPassword()`. Não desfaça isso.

Outros efeitos que viram `FALHA` na reentrega e disparam compensação indevida:

- `SolicitacaoService.marcarAprovada` (84–88): já `APROVADA` → `IllegalStateException("Solicitação não está PENDENTE")` → reply `FALHA`.
- `GerenteService.inativar` (74–78): já inativo → `IllegalStateException("Gerente não encontrado")` → reply `FALHA` → a compensação **reativa** o gerente. “Não encontrado” tem de ser só CPF ausente. Último gerente ativo continua `FALHA` com `GerenteRules.ULTIMO_ATIVO`.
- `AuthService.criarGerente`: CPF já criado por esta mesma saga não pode devolver e-mail duplicado. A senha do gerente veio do formulário e segue no payload da SAGA (não é gerada). Reentrega = `SUCESSO` sem corpo novo.

Mongo do compose é nó único: **não** use transação multi-documento (exige replica set). Postgres (cliente, gerente, conta) pode e deve usar uma transação.

**Correção:**

1. Auth, antes de `inbox.save`: remova a chave `senha` do JSON persistido. A reply **devolvida ao listener** (a que vai para `orquestrador.reply`) continua com `senha` na primeira execução. `capturePassword` não pode apagar um segredo já guardado quando a replay vier sem senha (hoje só faz `put` se não vazia — preserve isso).
2. Trave a idempotência pelo unique `(sagaId, tipo)` **antes** do efeito:
   - Mongo: `insert` do inbox primeiro (reply vazio / marcador, sem senha). `DuplicateKeyException` → leia o registro e devolva a reply armazenada se já estiver completa; se ainda estiver marcada “em progresso”, retome o efeito sem criar outro usuário.
   - Postgres: `@Transactional` no `handle` cobrindo efeito + `inbox.save`, no transaction manager certo. No MS Conta o inbox é do schema command; não use o transaction manager da query. Se `ContaCommandService` já abre transação, participe da mesma (`REQUIRED`), não commit o efeito e grave o inbox depois.
3. `criarCliente`: e-mail de **outro** CPF → continue `E-mail já cadastrado` (caso especial). Mesmo CPF já existente (reentrega) → `SUCESSO` com `cpf` e **sem** senha. Não regenere senha (o hash já está gravado; a senha em claro não pode ser reconstituída). Não use a mensagem de e-mail duplicado nesse ramo.
4. `criarGerente`: mesmo CPF → `SUCESSO`. E-mail de outro CPF → `E-mail já cadastrado`.
5. `marcarAprovada`: status já `APROVADA` → devolva a entidade (reply `SUCESSO` com o mesmo payload de dados). `NAO_APROVADA` ou inexistente → `FALHA`.
6. `inativar`: `ativo == false` → `SUCESSO` no-op. CPF ausente → `FALHA` “Gerente não encontrado”. Último ativo → `FALHA` com a mensagem atual de regra.
7. Não logue reply nem payload desses handlers.

**Teste:** teste de handler (pode ser unitário com repositório fake) para cada ramo acima. Um teste de `SagaEngine` já cobre e-mail duplicado de verdade (`SagaEngineTest`, caso não aprovada) — ele tem de continuar falhando a SAGA **só** quando o Auth devolve exatamente `E-mail já cadastrado` para outra pessoa. Acrescente um caso: reentrega de `auth.criar-cliente` depois do usuário criado não chama `marcar-nao-aprovada`.

### 4. ALTO — `ApiException` no comando de conta não vira reply

**Requisito:** §5.7. Erro de negócio no passo volta `FALHA` em `orquestrador.reply` para a SAGA compensar. Não pode ficar preso em requeue até o timeout de 30s.

**Atual:** `ContaCommandHandler.executar` só captura `IllegalStateException` (linhas 89–91). `atribuirGerente` → `requireExisting` lança `ApiException` (`ContaCommandService.kt` 251). O listener não captura → a mensagem requeueia. A SAGA só desiste no timeout.

**Correção:** no `executar` (ou no listener, desde que a reply `FALHA` seja publicada e a mensagem receba ack), capture `ApiException` e monte `falha(envelope, ex.body.mensagem, sagaId)`. Não inclua stack na reply. Mantenha o `IllegalStateException`.

**Teste:** comando `conta.atribuir-gerente` com número inexistente produz reply `status=FALHA` e não relança.

### 5. ALTO — Projeção CQRS se perde se o publish falha

**Requisito:** §5.10. O read model só acompanha o event store pela fila `ms.conta.events`. Depósito/saque/transferência não devolvem saldo; o front relê o query. Se o evento não chega, o saldo mente para sempre.

**Atual:** `ContaCommandService.publishAfterCommit` (277–288) publica depois do commit. `AmqpContaEventPublisher.publish` (17–36) engole qualquer `Exception` e, se não houver `RabbitTemplate`, retorna sem publicar. `removerSeSoCriado` (138–151) apaga o stream e só então publica o tombstone `Removido`. Publish perdido = command sem conta e query ainda com a conta.

O caminho feliz (Rabbit no ar) funciona. O defeito é a falha silenciosa.

**Correção:** outbox no schema `conta_command`, na **mesma transação** do `append` e do `deleteStream`:

- tabela `event_outbox` (id do evento, json do corpo já no formato que `ContaEventListener` lê: `id`, `objetoId`, `tipo`, `payload`, `versao`, `timestamp`, flag publicado);
- o `afterCommit` não é mais o único caminho. Um relay (scheduler curto no próprio MS Conta) lê não publicados, publica em `ms.conta.events` e marca. Falha de publish deixa a linha para a próxima volta. Não engula sumindo o evento.
- o tombstone `Removido` entra na outbox **antes** do commit que apaga o stream.
- não reenfileire `ms.conta.events.dlq` em código.

Não escreva no read model a partir do command, nem no reboot fora do `EventProjector.rebuild` que já existe.

**Teste:** com Rabbit indisponível no momento do commit, a linha fica na outbox; quando o broker volta, `projecao_aplicada` recebe o mesmo `evento_id` uma vez só. O teste de depósito sem saldo no body (`ContaIT`) continua válido.

### 6. MÉDIO — R11 omite cliente quando o saldo ainda não projetou

**Requisito:** R11. Lista todos os clientes com saldo, ordem pt-BR. Consistência do CQRS é eventual, mas sumir a linha não é o contrato.

**Atual:** `backend/gateway/src/routes/composition.ts` `composeClientes` 77–79 descarta a linha se `saldo` (ou cidade/estado) não vier. Logo depois da R9 o cliente existe e o saldo ainda não.

**Correção:** em `listarClientes`, se algum CPF da lista de clientes não tiver saldo em `/internal/saldos`, repita esse GET poucas vezes (3 tentativas, intervalo curto, só no Gateway). Ainda assim sem saldo: **não** invente `"0.00"`. Responda 503 `{ status, erro, mensagem }` deixando claro que a projeção não fechou, para o cliente HTTP tentar de novo. Cidade/estado ausentes continuam erro de cadastro (não são lag de CQRS); aí sim não monte linha mentirosa — prefira 502/503 do MS Cliente se o corpo vier sem endereço, em vez de lista silenciosamente menor.

**Teste:** estenda `composition` test do Gateway: saldo ausente na primeira resposta e presente na segunda entra na lista; saldo ausente nas três tentativas → 503, corpo de erro padrão, sem `_links` de sucesso.

### 7. MÉDIO — `cliente.criar` não exige solicitação aprovada

**Atual:** `CadastroService.criarAPartirDaSolicitacao` (31–35) cria a partir de qualquer status. Se o CPF já é cliente, devolve o existente (isso está certo para reentrega).

**Correção:** se não existe cliente e `solicitacao.status != APROVADA`, lance `IllegalStateException` (o handler já transforma em reply `FALHA`). Não mude o atalho de “já existe → devolve”.

### 8. MÉDIO — corrida de autocadastro vira 500

**Requisito:** R1 duplicidade de CPF ou e-mail → 409 `{ status, erro, mensagem }`.

**Atual:** `SolicitacaoService.criar` consulta antes do insert. O unique `solicitacao_email_uk` (`V1__cliente_schema.sql`) estoura `DataIntegrityViolationException` se duas requests passam juntas. `ClienteExceptionHandler` não mapeia isso → 500.

**Correção:** `@ExceptionHandler(DataIntegrityViolationException::class)` → `ErroBody.conflict` 409, mensagem estável (“CPF já cadastrado” ou “E-mail já cadastrado”, conforme a constraint). Não deixe o stack do Postgres vazar.

### 9. MÉDIO — PUT em gerente inativo

**Requisito:** R14 altera nome e telefone de gerente ativo. R15 deixa o removido inativo. O HATEOAS já esconde `atualizacao`, mas o HTTP aceita.

**Atual:** `GerenteService.atualizar` (21–40) não olha `ativo`. A checagem de CPF/e-mail imutáveis → 400 **já existe**; não a reimplemente no Gateway.

**Correção:** se `!atual.ativo`, `ErroBody.conflict("Gerente inativo")` 409 (ou 404, se preferir o mesmo texto de “não encontrado” usado na listagem — escolha 409 e cubra com teste).

### 10. MÉDIO — Gateway não espera SAGA nem Email

**Atual:** `docker-compose.yml` `gateway.depends_on` (149–161) não inclui `saga` nem `email`. Os dois já têm healthcheck `/health`.

**Correção:** `condition: service_healthy` para `saga` e `email`. Sem isso o Gateway aceita R9 enquanto `saga.cmd` ainda não tem consumidor.

### 11. MÉDIO — projeção idempotente só por `evento_id`

**Requisito:** §5.10. Reentrega do mesmo evento (id **ou** versão da conta) não altera saldo de novo.

**Atual:** `EventProjector` ignora `evento_id` já visto. O publisher reenvia o id do store, então a reentrega normal é segura. Um segundo UUID com a mesma `(objeto_id, versao)` somaria saldo de novo.

**Correção:** unique `(numero_conta, versao)` em `projecao_aplicada` (ou coluna equivalente já usada) e ignore o evento se a versão da conta já foi aplicada. Não troque o id na fila.

### 12. BAIXO — endurecimentos pequenos

Faça só se os itens 1–11 já estiverem verdes. Não misture no mesmo commit mental com a topologia.

- `backend/gateway/src/routes/jobs.ts` 38–42: `isJobOwner` devolve `true` se `job.cpf` é nulo. O Gateway grava `cpf` em R9/R13/R15/R16. Trate cpf ausente como 403. Ajuste o teste que cria job sem `cpf`. Em `SagaEngine.start` (54–60) não recrie job sem `cpf` por cima de um job existente; o `copy` do `complete` já preserva campos — não zere `cpf`.
- `backend/gateway/src/routes/proxy.ts` 145–147: `transferir` responde HTTP 401 com corpo `Erros.forbidden()` (`status: 403`). O hook já exige usuário. Apague o ramo ou responda 401 com `{ auth: false, message: "Falha ao autenticar o token." }`.
- `backend/gateway/src/routes/logout.ts` 7–9: `if (!user) return` não envia resposta. Apague o ramo (o hook responde 401) ou envie o 401 de token inválido.
- `SagaRegistry` registra a SAGA `echo` no bean de produção (linhas 10 e 15–32). O Gateway não publica esse tipo. Remova `echo()` da lista do `@Component` **somente** se `SagaIT` não depender do bean para esse tipo; o companion `echo()` pode ficar para teste. Não apague o teste de timeout/DLQ junto.
- `EventReplay.dinheiro` aceita valor não-string. Os escritores usam `Money.format`. Pode passar a exigir `String` `^\d+\.\d{2}$` e falhar o replay. Não é o bug do caminho de escrita.

---

## O que esta leitura considerou conforme

Não reabra sem uma falha de teste:

- Rotas do Swagger no Gateway, incluindo R9 sem pré-validação (inexistente/já aprovada → 202 + job `FALHA`), R10 síncrono, R15 auto-remoção 403 antes da SAGA, R16 job inline.
- Mensagens 401 exatas, sessão `sessao:<jti>` + `sessao:cpf:<cpf>`, logout, JWT só no Gateway, Argon2id, login composição `{ cpf, nome, email }` sem `_links` e sem senha.
- Enriquecimento da transferência (R6) no Gateway antes do POST ao command.
- HATEOAS rewrite para a URL pública; login, jobs, health e reboot sem `_links`.
- Ordem das SAGAs R9 e R15 em `SagaRegistry`, skip `semConta` / `semContas`, timeout 30s, guard de compensação DLQ+timeout, senha fora do Redis da SAGA.
- Eventos `Criado`, `Saque`, `Depósito`, `TransferênciaOrigem`, `TransferênciaDestino`, `GerenteAlterado`; unique `(objeto_id, versao)`; saldo por replay; conta nova aleatória de 4 dígitos; R13 escolhe a conta de menor saldo do gerente com mais contas e não esvazia gerente; R15 transfere para o ativo com menos clientes.
- Seed 5 clientes / 4 gerentes / saldos do enunciado e reboot que repõe command e query.
- Schemas Postgres, ICU `pt-BR-x-icu`, `mem_limit`, `JAVA_TOOL_OPTIONS`, `NODE_OPTIONS`, `shared_buffers`, WiredTiger, Redis `noeviction`. MSs sem porta publicada; Gateway em 3000.
- `NUMERIC(19,4)` e JSON de dinheiro como string.

## Verificação final

1. Volume Rabbit recriado; filas do item 1 visíveis.
2. `docker compose ps`: postgres, mongo, redis, rabbit, auth, cliente, gerente, conta, saga, email, gateway healthy.
3. Testes de módulo dos pontos tocados (Auth handler, Cliente, Gerente, Conta command/projector, SagaEngine, Gateway composition/jobs).
4. `pytest` em `backend/contract-tests` contra `http://localhost:3000`, começando por `POST /reboot`. A suíte cobre T00–T16; esta auditoria não a rodou, então uma falha nova ali entra neste relatório antes de “dar por corrigido”.
5. Não commitar segredo. Não commitar salvo pedido explícito.
