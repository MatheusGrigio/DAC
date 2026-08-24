import cors from '@fastify/cors';
import Fastify, { type FastifyInstance } from 'fastify';
import { noopSagaPublisher, type SagaPublisher } from './amqp/publisher.js';
import { loadConfig, type AppConfig } from './config.js';
import { registerAuthHook } from './auth/hook.js';
import { connectRedis, RedisStore } from './redis/redis-store.js';
import type { KeyValueStore } from './redis/store.js';
import type { MsFetch } from './http/ms-client.js';
import { registerAprovacao } from './routes/aprovacao.js';
import { registerInserirGerente } from './routes/inserir-gerente.js';
import { registerRemoverGerente } from './routes/remover-gerente.js';
import { registerJobs } from './routes/jobs.js';
import { registerRelatorio } from './routes/relatorio.js';
import { registerLogin } from './routes/login.js';
import { registerLogout } from './routes/logout.js';
import { registerProxy } from './routes/proxy.js';
import { registerReboot } from './routes/reboot.js';
import './types/fastify.js';

export type BuildAppOptions = {
  config?: AppConfig;
  store?: KeyValueStore;
  fetchImpl?: MsFetch;
  publisher?: SagaPublisher;
};

export async function buildApp(options: BuildAppOptions = {}): Promise<FastifyInstance> {
  const config = options.config ?? loadConfig();
  const store = options.store ?? new RedisStore(connectRedis(config.redisUrl));
  const publisher = options.publisher ?? noopSagaPublisher;
  const app = Fastify({ logger: options.store === undefined });

  await app.register(cors, {
    origin: config.corsOrigin,
    methods: ['GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'OPTIONS'],
    allowedHeaders: ['Content-Type', 'Accept', 'x-access-token'],
    exposedHeaders: ['Location'],
  });

  registerAuthHook(app, { store, jwtSecret: config.jwtSecret });

  app.get('/health', async () => ({ status: 'UP' }));
  registerLogin(app, { config, store, fetchImpl: options.fetchImpl });
  registerLogout(app, store);
  registerReboot(app, { config, store, fetchImpl: options.fetchImpl });
  registerAprovacao(app, { store, publisher });
  registerInserirGerente(app, { store, publisher });
  registerRemoverGerente(app, { store, publisher });
  registerRelatorio(app, { config, store, fetchImpl: options.fetchImpl });
  registerJobs(app, store);
  registerProxy(app, { config, store, fetchImpl: options.fetchImpl });

  return app;
}
