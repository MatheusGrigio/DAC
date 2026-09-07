import { randomUUID } from 'node:crypto';
import type { FastifyInstance, FastifyReply, FastifyRequest } from 'fastify';
import type { AppConfig } from '../config.js';
import { saveJob } from '../redis/jobs.js';
import type { KeyValueStore } from '../redis/store.js';
import type { MsFetch } from '../http/ms-client.js';
import { JobStatus, ResultType } from '../types/enums.js';
import { collectRelatorioClientes, identityHeadersFrom } from './composition.js';

type RelatorioDeps = {
  config: AppConfig;
  store: KeyValueStore;
  fetchImpl?: MsFetch;
};

export function registerRelatorio(app: FastifyInstance, deps: RelatorioDeps): void {
  app.get('/relatorios/clientes', async (request: FastifyRequest, reply: FastifyReply) => {
    const jobId = randomUUID();
    const accepted = { jobId, status: JobStatus.PENDENTE };
    await saveJob(deps.store, { ...accepted, cpf: request.user?.cpf });
    const headers = identityHeadersFrom(request.user);
    setImmediate(() => {
      void runRelatorio(deps, jobId, headers);
    });
    reply.header('Location', `/jobs/${jobId}/status`);
    return reply.code(202).send(accepted);
  });
}

async function runRelatorio(
  deps: RelatorioDeps,
  jobId: string,
  headers: Record<string, string>,
): Promise<void> {
  const collected = await collectRelatorioClientes(deps, headers);
  if (!collected.ok) {
    await saveJob(deps.store, {
      jobId,
      status: JobStatus.FALHA,
      erro: collected.erro,
      cpf: ownerCpf(headers),
    });
    return;
  }
  await saveJob(deps.store, {
    jobId,
    status: JobStatus.CONCLUIDO,
    resultType: ResultType.INLINE,
    resultado: collected.body,
    cpf: ownerCpf(headers),
  });
}

function ownerCpf(headers: Record<string, string>): string | undefined {
  return headers['X-User-CPF'];
}
