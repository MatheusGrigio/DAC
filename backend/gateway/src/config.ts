export const SESSION_TTL_SECONDS = 30 * 60;
export const JWT_EXPIRES_IN = '8h' as const;
export const MS_TIMEOUT_MS = 5_000;
export const CACHE_TTL_SECONDS = 5 * 60;
export const JOB_TTL_SECONDS = 5 * 60;

export type AppConfig = {
  port: number;
  host: string;
  jwtSecret: string;
  publicUrl: string;
  corsOrigin: string;
  redisUrl: string;
  rabbitUrl: string;
  authUrl: string;
  clienteUrl: string;
  gerenteUrl: string;
  contaUrl: string;
};

export function loadConfig(env: NodeJS.ProcessEnv = process.env): AppConfig {
  const redisHost = env.REDIS_HOST ?? '127.0.0.1';
  const redisPort = env.REDIS_PORT ?? '6379';
  const rabbitHost = env.RABBIT_HOST ?? '127.0.0.1';
  const rabbitPort = env.RABBIT_PORT ?? '5672';
  const rabbitUser = env.RABBIT_USER ?? 'bantads';
  const rabbitPassword = env.RABBIT_PASSWORD ?? 'change-me';
  return {
    port: Number(env.PORT ?? 3000),
    host: env.HOST ?? '0.0.0.0',
    jwtSecret: env.JWT_SECRET ?? 'change-me-to-a-long-random-string',
    publicUrl: (env.GATEWAY_PUBLIC_URL ?? 'http://localhost:3000').replace(/\/$/, ''),
    corsOrigin: env.CORS_ORIGIN ?? 'http://localhost:4200',
    redisUrl: env.REDIS_URL ?? `redis://${redisHost}:${redisPort}`,
    rabbitUrl:
      env.RABBIT_URL ??
      `amqp://${encodeURIComponent(rabbitUser)}:${encodeURIComponent(rabbitPassword)}@${rabbitHost}:${rabbitPort}`,
    authUrl: (env.AUTH_URL ?? 'http://localhost:8081').replace(/\/$/, ''),
    clienteUrl: (env.CLIENTE_URL ?? 'http://localhost:8082').replace(/\/$/, ''),
    gerenteUrl: (env.GERENTE_URL ?? 'http://localhost:8083').replace(/\/$/, ''),
    contaUrl: (env.CONTA_URL ?? 'http://localhost:8084').replace(/\/$/, ''),
  };
}
