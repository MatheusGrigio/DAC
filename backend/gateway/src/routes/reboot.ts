import type { FastifyInstance } from 'fastify';
import type { AppConfig } from '../config.js';
import { msRequest, type MsFetch } from '../http/ms-client.js';
import type { KeyValueStore } from '../redis/store.js';

function countOf(body: unknown, key: string): number {
  if (!body || typeof body !== 'object') {
    return 0;
  }
  const value = (body as Record<string, unknown>)[key];
  return typeof value === 'number' ? value : 0;
}

export function registerReboot(
  app: FastifyInstance,
  deps: { config: AppConfig; store: KeyValueStore; fetchImpl?: MsFetch },
): void {
  app.post('/reboot', async (_request, reply) => {
    const targets = [
      { url: deps.config.authUrl },
      { url: deps.config.clienteUrl },
      { url: deps.config.gerenteUrl },
      { url: deps.config.contaUrl },
    ];
    const results = await Promise.all(
      targets.map((target) =>
        msRequest({
          baseUrl: target.url,
          method: 'POST',
          path: '/internal/reboot',
          timeoutMs: 60_000,
          fetchImpl: deps.fetchImpl,
        }),
      ),
    );
    if (results.some((item) => item.status < 200 || item.status >= 300)) {
      return reply.code(500).send({
        status: 500,
        erro: 'Internal Server Error',
        mensagem: 'Falha ao recriar o seed',
      });
    }
    await deps.store.flushdb();
    const cliente = results[1];
    const gerente = results[2];
    const conta = results[3];
    return reply.code(200).send({
      status: 'ok',
      clientes: countOf(cliente?.body, 'clientes'),
      gerentes: countOf(gerente?.body, 'gerentes'),
      contas: countOf(conta?.body, 'contas'),
    });
  });
}
