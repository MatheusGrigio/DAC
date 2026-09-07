import { randomUUID } from 'node:crypto';
import type { FastifyInstance, FastifyReply, FastifyRequest } from 'fastify';
import type { SagaPublisher } from '../amqp/publisher.js';
import { Erros } from '../http/errors.js';
import { nowTimestamp } from '../http/dates.js';
import { saveJob } from '../redis/jobs.js';
import type { KeyValueStore } from '../redis/store.js';
import { CommandTypes } from '../types/command-types.js';
import { JobStatus } from '../types/enums.js';

type RemoverDeps = {
  store: KeyValueStore;
  publisher: SagaPublisher;
};

export function registerRemoverGerente(app: FastifyInstance, deps: RemoverDeps): void {
  app.delete('/gerentes/:cpf', async (request: FastifyRequest, reply: FastifyReply) => {
    const cpf = (request.params as { cpf: string }).cpf;
    if (request.user?.cpf === cpf) {
      return reply.code(403).send(Erros.forbidden('Não é permitido remover a si mesmo'));
    }
    const jobId = randomUUID();
    const accepted = { jobId, status: JobStatus.PENDENTE };
    const job = { ...accepted, cpf: request.user?.cpf };
    await saveJob(deps.store, job);
    try {
      await deps.publisher.publish({
        sagaId: jobId,
        tipo: CommandTypes.REMOVER_GERENTE,
        timestamp: nowTimestamp(),
        payload: { cpf },
      });
    } catch {
      await saveJob(deps.store, {
        ...job,
        status: JobStatus.FALHA,
        erro: 'Falha ao publicar SAGA',
      });
    }
    reply.header('Location', `/jobs/${jobId}/status`);
    return reply.code(202).send(accepted);
  });
}
