# Tutorial — Redis no API Gateway (BANTADS)

Este documento explica a pasta `backend/gateway/src/redis/`: propósito de cada arquivo, cronologia dos eventos no sistema, cada função/bloco de código e **quem chama o quê** (e por quê).

O Redis no gateway serve a **três papéis** distintos, todos via a mesma conexão:


| Papel                            | Módulo       | Chaves (exemplos)                                    | TTL típico (`config.ts`) |
| -------------------------------- | ------------ | ---------------------------------------------------- | ------------------------ |
| Sessão JWT (login único, logout) | `session.ts` | `sessao:{jti}`, `sessao:cpf:{cpf}`, `revogado:{jti}` | 30 min (renovável)       |
| Cache de cadastro                | `cache.ts`   | `cache:cliente:{cpf}`, `cache:gerente:{cpf}`         | 5 min                    |
| Jobs 202 (SAGA + relatório)      | `jobs.ts`    | `job:{jobId}`                                        | 5 min                    |


O **orquestrador SAGA** (`services/saga`) grava no **mesmo** prefixo `job:{jobId}` no Redis compartilhado; o gateway só cria o job inicial e lê status/resultado nas rotas `/jobs/`*.

---



## Ordem de leitura dos arquivos (dependências)

1. `store.ts` — contrato abstrato (interface)-
2. `redis-store.ts` — implementação real com **ioredis**
3. `session.ts` — autenticação stateful no gateway
4. `cache.ts` — cache HTTP de GET de cliente/gerente
5. `jobs.ts` — estado de jobs assíncronos

Em produção, `app.ts` instancia `RedisStore(connectRedis(url))` e injeta `KeyValueStore` em hooks e rotas. Nos testes, `gateway/test/memory-store.ts` implementa a mesma interface em memória.

---



## Cronologia dos eventos (visão global)

```text
Subida do gateway
  → connectRedis + RedisStore (app.ts)
  → registerAuthHook recebe store

POST /login (público)
  → MS Auth OK → sign JWT → createSession (session.ts)
  → Redis: sessao:{jti}, sessao:cpf:{cpf}

Request autenticada (qualquer rota protegida)
  → hook: verify JWT → isRevoked → readSession → touchSession
  → request.user preenchido

POST /logout
  → revokeSession → apaga sessão + revogado:{jti} até exp do JWT

GET /clientes/:cpf ou GET /gerentes/:cpf
  → readCache hit → resposta sem MS
  → miss → MS → writeCache

POST … 202 (SAGA ou relatório)
  → saveJob PENDENTE
  → (SAGA) MS Saga atualiza job no Redis
  → (relatório) gateway atualiza job no background
  → cliente faz polling GET /jobs/:id/status e /result

POST /reboot (público)
  → MSs internal/reboot → store.flushdb() (zera TODO o Redis do gateway)
```

---



## 1. `store.ts` — contrato `KeyValueStore`

**Propósito:** isolar o restante do gateway do cliente Redis. Rotas, sessão, cache e jobs dependem só desta interface — facilita testes com `MemoryStore`.

```1:8:backend/gateway/src/redis/store.ts
export interface KeyValueStore {
  get(key: string): Promise<string | null>;
  set(key: string, value: string, ttlSeconds?: number): Promise<void>;
  del(...keys: string[]): Promise<number>;
  expire(key: string, ttlSeconds: number): Promise<void>;
  ttl(key: string): Promise<number>;
  flushdb(): Promise<void>;
}
```


| Método                 | Uso no gateway                                               |
| ---------------------- | ------------------------------------------------------------ |
| `get` / `set`          | Valores string (JSON serializado em session, cache, jobs)    |
| `set(..., ttlSeconds)` | Expiração automática (sessão, cache, jobs, revogados)        |
| `del`                  | Logout, invalidar cache, limpar chave corrompida             |
| `expire`               | `touchSession` — renova TTL sem regravar JSON                |
| `ttl`                  | Disponível na interface; uso direto limitado no código atual |
| `flushdb`              | `/reboot` — reset total após seed dos MSs                    |


**Quem importa o tipo:** praticamente todo módulo que recebe `store` em `app.ts` (`hook.ts`, `login.ts`, `logout.ts`, `proxy.ts`, `jobs.ts`, rotas SAGA, `relatorio.ts`, `reboot.ts`, `composition.ts` só no tipo de deps).

---



## 2. `redis-store.ts` — Redis real (ioredis)

**Propósito:** implementar `KeyValueStore` e abrir conexão com `REDIS_URL`.

### Classe `RedisStore`

```4:36:backend/gateway/src/redis/redis-store.ts
export class RedisStore implements KeyValueStore {
  constructor(private readonly redis: Redis) {}

  get(key: string): Promise<string | null> {
    return this.redis.get(key);
  }

  async set(key: string, value: string, ttlSeconds?: number): Promise<void> {
    if (ttlSeconds === undefined) {
      await this.redis.set(key, value);
      return;
    }
    await this.redis.set(key, value, 'EX', ttlSeconds);
  }

  del(...keys: string[]): Promise<number> {
    if (keys.length === 0) {
      return Promise.resolve(0);
    }
    return this.redis.del(...keys);
  }

  async expire(key: string, ttlSeconds: number): Promise<void> {
    await this.redis.expire(key, ttlSeconds);
  }

  ttl(key: string): Promise<number> {
    return this.redis.ttl(key);
  }

  async flushdb(): Promise<void> {
    await this.redis.flushdb();
  }
}
```

- `set` **sem TTL:** chave permanente até `del` ou `flushdb` (não usado pelos módulos de domínio atuais, que sempre passam TTL).
- `del` **vazio:** evita chamar Redis com zero chaves.
- `flushdb`**:** apaga o banco Redis **atual** (no compose, o DB usado pelo gateway).

**Quem chama:** `app.ts` cria `new RedisStore(connectRedis(config.redisUrl))` quando não há `options.store` (testes injetam mock).

```27:29:backend/gateway/src/app.ts
export async function buildApp(options: BuildAppOptions = {}): Promise<FastifyInstance> {
  const config = options.config ?? loadConfig();
  const store = options.store ?? new RedisStore(connectRedis(config.redisUrl));
```



### `connectRedis(url)`

```39:44:backend/gateway/src/redis/redis-store.ts
export function connectRedis(url: string): Redis {
  return new Redis(url, {
    maxRetriesPerRequest: 2,
    enableReadyCheck: true,
  });
}
```

**Propósito:** cliente **ioredis** com poucas retentativas por request e checagem de “ready” antes de usar.

**Quem chama:** só `app.ts` na linha acima.

---



## 3. `session.ts` — sessão, login único e revogação

**Propósito:** o JWT prova identidade, mas o gateway exige **sessão ativa no Redis** (R1/login único: novo login invalida sessão anterior do mesmo CPF). Logout remove sessão e marca `jti` como revogado até o JWT expirar.

Constante: `SESSION_TTL_SECONDS = 30 * 60` em `config.ts` (30 minutos, renovados a cada request autenticada).

### Tipo `SessionRecord`

```5:9:backend/gateway/src/redis/session.ts
export type SessionRecord = {
  cpf: string;
  tipo: GateQ2wayUser['tipo'];
  expJwt: number;
};
```

Espelho do que importa na sessão (não guarda `jti` dentro do JSON — o `jti` está na chave).

### Funções de nome de chave

```11:21:backend/gateway/src/redis/session.ts
export function sessionKey(jti: string): string {
  return `sessao:${jti}`;
}

export function sessionByCpfKey(cpf: string): string {
  return `sessao:cpf:${cpf}`;
}

export function revokedKey(jti: string): string {
  return `revogado:${jti}`;
}
```


| Função                 | Chave              | Conteúdo                         |
| ---------------------- | ------------------ | -------------------------------- |
| `sessionKey(jti)`      | `sessao:{jti}`     | JSON `SessionRecord`             |
| `sessionByCpfKey(cpf)` | `sessao:cpf:{cpf}` | string = `jti` ativo daquele CPF |
| `revokedKey(jti)`      | `revogado:{jti}`   | `"1"` — blacklist pós-logout     |


Exportadas para testes/documentação; uso interno + fluxo de sessão.

### `createSession(store, user)`

```23:34:backend/gateway/src/redis/session.ts
export async function createSession(
  store: KeyValueStore,
  user: Pick<GatewayUser, 'cpf' | 'tipo' | 'jti' | 'exp'>,
): Promise<void> {
  const previous = await store.get(sessionByCpfKey(user.cpf));
  if (previous && previous !== user.jti) {
    await store.del(sessionKey(previous));
  }
  const record: SessionRecord = { cpf: user.cpf, tipo: user.tipo, expJwt: user.exp };
  await store.set(sessionKey(user.jti), JSON.stringify(record), SESSION_TTL_SECONDS);
  await store.set(sessionByCpfKey(user.cpf), user.jti, SESSION_TTL_SECONDS);
}
```

**Ordem interna:**

1. Lê `jti` anterior do CPF → se existir e for outro, **apaga** `sessao:{jtiAntigo}` (login único).
2. Grava registro da nova sessão e ponteiro CPF → `jti`.

**Quem chama:** `routes/login.ts` após `signAccessToken` e `tokenExpiry` — porque só após senha válida o gateway emite token **e** registra sessão.

```76:79:backend/gateway/src/routes/login.ts
    const jti = randomUUID();
    const token = signAccessToken(deps.config.jwtSecret, { cpf, tipo: tipoRaw, jti });
    const exp = tokenExpiry(token);
    await createSession(deps.store, { cpf, tipo: tipoRaw, jti, exp });
```



### `touchSession(store, cpf, jti)`

```36:39:backend/gateway/src/redis/session.ts
export async function touchSession(store: KeyValueStore, cpf: string, jti: string): Promise<void> {
  await store.expire(sessionKey(jti), SESSION_TTL_SECONDS);
  await store.expire(sessionByCpfKey(cpf), SESSION_TTL_SECONDS);
}
```

**Propósito:** “sliding expiration” — usuário ativo não perde sessão aos 30 min se continuar usando a API.

**Quem chama:** `auth/hook.ts` **depois** de `isRevoked` e `readSession` OK, **antes** de `request.user = user` — só renova sessões válidas.

```35:44:backend/gateway/src/auth/hook.ts
    if (await isRevoked(deps.store, user.jti)) {
      return reply.code(401).send(authError(AuthMessages.TOKEN_INVALIDO));
    }
    const session = await readSession(deps.store, user.jti);
    if (!session) {
      return reply.code(401).send(authError(AuthMessages.TOKEN_INVALIDO));
    }

    await touchSession(deps.store, user.cpf, user.jti);
    request.user = user;
```



### `revokeSession(store, user, nowSeconds)`

```41:49:backend/gateway/src/redis/session.ts
export async function revokeSession(
  store: KeyValueStore,
  user: Pick<GatewayUser, 'cpf' | 'jti' | 'exp'>,
  nowSeconds: number,
): Promise<void> {
  await store.del(sessionKey(user.jti), sessionByCpfKey(user.cpf));
  const remaining = Math.max(1, user.exp - nowSeconds);
  await store.set(revokedKey(user.jti), '1', remaining);
}
```

**Propósito:** logout — remove sessão ativa e bloqueia o mesmo JWT até `exp` (mesmo que alguém guarde o token).

**Quem chama:** `routes/logout.ts` com `request.user` do hook.

```6:11:backend/gateway/src/routes/logout.ts
  app.post('/logout', async (request, reply) => {
    const user = request.user;
    if (!user) {
      return;
    }
    await revokeSession(store, user, Math.floor(Date.now() / 1000));
```



### `isRevoked(store, jti)`

```51:53:backend/gateway/src/redis/session.ts
export async function isRevoked(store: KeyValueStore, jti: string): Promise<boolean> {
  return (await store.get(revokedKey(jti))) !== null;
}
```

**Quem chama:** `auth/hook.ts` **antes** de `readSession` — token com assinatura válida mas logout feito deve falhar.

### `readSession(store, jti)`

```55:64:backend/gateway/src/redis/session.ts
export async function readSession(
  store: KeyValueStore,
  jti: string,
): Promise<SessionRecord | null> {
  const raw = await store.get(sessionKey(jti));
  if (!raw) {
    return null;
  }
  return JSON.parse(raw) as SessionRecord;
}
```

**Quem chama:** `auth/hook.ts` — sem chave `sessao:{jti}`, 401 (token órfão ou sessão expirada).

### Cronologia sessão (resumo)

```text
Login  → createSession
Cada API autenticada → isRevoked → readSession → touchSession
Logout → revokeSession
Novo login mesmo CPF → createSession apaga sessao:{jtiAntigo}
```

---



## 4. `cache.ts` — cache de cadastro (cliente/gerente)

**Propósito:** reduzir chamadas aos MS Cliente/Gerente em GET por CPF. TTL: `CACHE_TTL_SECONDS` (5 min).

### Chaves

```4:10:backend/gateway/src/redis/cache.ts
export function clienteCacheKey(cpf: string): string {
  return `cache:cliente:${cpf}`;
}

export function gerenteCacheKey(cpf: string): string {
  return `cache:gerente:${cpf}`;
}
```

**Quem chama as chaves:** `routes/proxy.ts` ao registrar GET `/clientes/:cpf` e GET `/gerentes/:cpf`; testes em `gateway/test/hateoas.test.ts` (`clienteCacheKey`).

### `readCache(store, key)`

```12:23:backend/gateway/src/redis/cache.ts
export async function readCache(store: KeyValueStore, key: string): Promise<unknown | null> {
  const raw = await store.get(key);
  if (!raw) {
    return null;
  }
  try {
    return JSON.parse(raw) as unknown;
  } catch {
    await store.del(key);
    return null;
  }
}
```

**Propósito:** hit → objeto JS; JSON inválido → trata como miss e remove chave.

**Quem chama:** `proxy.ts` → `cachedGet` no início — se hit, responde 200 com HATEOAS sem ir ao MS.

```91:99:backend/gateway/src/routes/proxy.ts
  const hit = await readCache(deps.store, cacheKey);
  if (hit) {
    reply.code(200).send(
      applyHateoas(hit, {
        publicUrl: deps.config.publicUrl,
        user: request.user,
        requestUrl: request.url.startsWith('/') ? request.url : `/${request.url}`,
      }),
    );
```



### `writeCache(store, key, value)`

```25:27:backend/gateway/src/redis/cache.ts
export async function writeCache(store: KeyValueStore, key: string, value: unknown): Promise<void> {
  await store.set(key, JSON.stringify(value), CACHE_TTL_SECONDS);
}
```

**Quem chama:** `proxy.ts` → `cachedGet` após MS 200 e `cacheableCadastro`.

```107:108:backend/gateway/src/routes/proxy.ts
  const stored = cacheableCadastro(forwarded.body, deps.config.publicUrl);
  await writeCache(deps.store, cacheKey, stored);
```



### Invalidação manual (fora de `cache.ts`)

`PUT /gerentes/:cpf` apaga cache do gerente se o MS retornar 200:

```225:230:backend/gateway/src/routes/proxy.ts
  app.put('/gerentes/:cpf', async (request, reply) => {
    const cpf = (request.params as { cpf: string }).cpf;
    const forwarded = await forward(request, deps, deps.config.gerenteUrl);
    if (forwarded.status === 200) {
      await deps.store.del(gerenteCacheKey(cpf));
    }
```

**Por quê:** `store.del` direto — não há helper `invalidateCache`; a rota conhece a chave via `gerenteCacheKey`.

---



## 5. `jobs.ts` — estado de jobs HTTP 202

**Propósito:** persistir jobs para polling (`GET /jobs/:jobId/status` e `/result`). Mesma chave `job:{jobId}` usada pelo **MS Saga** ao concluir SAGAs.

TTL padrão: `JOB_TTL_SECONDS` (5 min).

### Tipo `StoredJob`

```4:13:backend/gateway/src/redis/jobs.ts
export type StoredJob = {
  jobId: string;
  status: string;
  cpf?: string | null;
  resultType?: string | null;
  dominio?: string | null;
  resourceId?: string | null;
  resultado?: Record<string, unknown> | null;
  erro?: string | null;
};
```

`cpf` no job = CPF do **dono** (gerente autenticado) para `isJobOwner` em `jobs.ts` (rota).

### `jobKey(jobId)`

```15:17:backend/gateway/src/redis/jobs.ts
export function jobKey(jobId: string): string {
  return `job:${jobId}`;
}
```

**Quem usa:** `saveJob` / `readJob`; testes (`jobs.test.ts`, `relatorio.test.ts`); alinhado a `RedisJobStore.key` no saga.

### `saveJob(store, job, ttlSeconds?)`

```19:25:backend/gateway/src/redis/jobs.ts
export async function saveJob(
  store: KeyValueStore,
  job: StoredJob,
  ttlSeconds = JOB_TTL_SECONDS,
): Promise<void> {
  await store.set(jobKey(job.jobId), JSON.stringify(job), ttlSeconds);
}
```


| Chamador                       | Momento                  | Por quê                                                  |
| ------------------------------ | ------------------------ | -------------------------------------------------------- |
| `aprovacao.ts`                 | 202 SAGA aprovar cliente | Job `PENDENTE`; se publish falhar → `FALHA`              |
| `inserir-gerente.ts`           | 202 SAGA inserir gerente | Idem                                                     |
| `remover-gerente.ts`           | 202 SAGA remover gerente | Idem                                                     |
| `relatorio.ts`                 | 202 relatório            | `PENDENTE` na entrada; background `CONCLUIDO` ou `FALHA` |
| MS Saga (`RedisJobStore.save`) | Durante SAGA             | Atualiza status/resultado no mesmo `jobId`               |


Exemplo gateway (aprovacao):

```18:21:backend/gateway/src/routes/aprovacao.ts
    const jobId = randomUUID();
    const accepted = { jobId, status: JobStatus.PENDENTE };
    const job = { ...accepted, cpf: request.user?.cpf };
    await saveJob(deps.store, job);
```



### `readJob(store, jobId)`

```27:38:backend/gateway/src/redis/jobs.ts
export async function readJob(store: KeyValueStore, jobId: string): Promise<StoredJob | null> {
  const raw = await store.get(jobKey(jobId));
  if (!raw) {
    return null;
  }
  try {
    return JSON.parse(raw) as StoredJob;
  } catch {
    await store.del(jobKey(jobId));
    return null;
  }
}
```

**Quem chama:** `routes/jobs.ts` — status e result; 404 se null/expirado.

```9:12:backend/gateway/src/routes/jobs.ts
    const jobId = (request.params as { jobId: string }).jobId;
    const job = await readJob(store, jobId);
    if (!job) {
      return reply.code(404).send(Erros.notFound('Job inexistente ou expirado'));
```



### `jobStatusBody(job)`

```40:55:backend/gateway/src/redis/jobs.ts
export function jobStatusBody(job: StoredJob): Record<string, unknown> {
  const body: Record<string, unknown> = { jobId: job.jobId, status: job.status };
  if (job.resultType != null) {
    body.resultType = job.resultType;
  }
  if (job.dominio != null) {
    body.dominio = job.dominio;
  }
  if (job.resourceId != null) {
    body.resourceId = job.resourceId;
  }
  if (job.erro != null) {
    body.erro = job.erro;
  }
  return body;
}
```

**Propósito:** corpo do contrato em `GET .../status` — omite campos nulos; **não** inclui `resultado` (isso vai em `/result`).

**Quem chama:** `routes/jobs.ts` na rota de status.

### Cronologia job SAGA (exemplo)

```text
1. Gateway POST → saveJob PENDENTE + publish RabbitMQ (jobId = sagaId)
2. Cliente poll GET /jobs/{id}/status → readJob
3. Saga orquestra → RedisJobStore.save atualiza status/resultType/dominio/...
4. Cliente GET /jobs/{id}/result quando CONCLUIDO + INLINE
```

Relatório: passos 1–2 iguais; passo 3 é `runRelatorio` no gateway (`saveJob` CONCLUIDO/FALHA), sem saga.

---



## 6. Uso transversal: `flushdb` no `/reboot`

Não há arquivo dedicado — `reboot.ts` chama o contrato diretamente:

```43:43:backend/gateway/src/routes/reboot.ts
    await deps.store.flushdb();
```

**Ordem:** primeiro `POST /internal/reboot` em Auth, Cliente, Gerente, Conta; se todos 2xx, **limpa Redis** (sessões, cache, jobs, revogados). **Por quê:** seed novo + estado ephemeral coerente com contrato de aceite.

---



## Mapa rápido: função → chamadores


| Função / método                            | Chamadores principais                                                        |
| ------------------------------------------ | ---------------------------------------------------------------------------- |
| `KeyValueStore` (tipo)                     | `app.ts`, todas as rotas, `hook.ts`, testes                                  |
| `RedisStore` / `connectRedis`              | `app.ts` (produção)                                                          |
| `createSession`                            | `login.ts`                                                                   |
| `touchSession`, `readSession`, `isRevoked` | `hook.ts`                                                                    |
| `revokeSession`                            | `logout.ts`                                                                  |
| `readCache`, `writeCache`                  | `proxy.ts` (`cachedGet`)                                                     |
| `clienteCacheKey`, `gerenteCacheKey`       | `proxy.ts`, testes                                                           |
| `saveJob`                                  | `aprovacao`, `inserir-gerente`, `remover-gerente`, `relatorio` + **MS Saga** |
| `readJob`, `jobStatusBody`                 | `routes/jobs.ts`                                                             |
| `flushdb`                                  | `reboot.ts`                                                                  |
| `store.del(gerenteCacheKey)`               | `proxy.ts` (PUT gerente)                                                     |


---



## Referência de TTL (`backend/gateway/src/config.ts`)

```1:5:backend/gateway/src/config.ts
export const SESSION_TTL_SECONDS = 30 * 60;
export const JWT_EXPIRES_IN = '8h' as const;
export const MS_TIMEOUT_MS = 5_000;
export const CACHE_TTL_SECONDS = 5 * 60;
export const JOB_TTL_SECONDS = 5 * 60;
```

- **JWT** dura até 8h; **sessão Redis** renova a cada 30 min de inatividade máxima por `touchSession`.
- **Revogado** dura `exp - now` do JWT (ver `revokeSession`).
- Cache e jobs expiram em 5 min se não renovados (`saveJob` regrava com TTL novo).

---



## Arquivos relacionados fora de `redis/`


| Arquivo                              | Relação com Redis                                |
| ------------------------------------ | ------------------------------------------------ |
| `auth/hook.ts`                       | Sessão em cada request                           |
| `auth/jwt.ts`                        | Sem Redis; fornece `jti`/`exp` para `session.ts` |
| `routes/login.ts` / `logout.ts`      | Criar / revogar sessão                           |
| `routes/proxy.ts`                    | Cache GET + invalidação                          |
| `routes/jobs.ts`                     | Leitura de jobs                                  |
| `routes/reboot.ts`                   | `flushdb`                                        |
| `gateway/test/memory-store.ts`       | Implementação fake de `KeyValueStore`            |
| `services/saga/.../RedisJobStore.kt` | Escreve `job:{id}` compartilhado                 |


Este tutorial cobre apenas o código em `backend/gateway/src/redis/` e seus pontos de integração diretos no gateway.