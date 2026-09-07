import { randomUUID } from 'node:crypto';
import type { FastifyInstance, FastifyReply, FastifyRequest } from 'fastify';
import type { SagaPublisher } from '../amqp/publisher.js';
import { nowTimestamp } from '../http/dates.js';
import { saveJob } from '../redis/jobs.js';
import type { KeyValueStore } from '../redis/store.js';
import { CommandTypes } from '../types/command-types.js';
import { JobStatus } from '../types/enums.js';

type AprovacaoDeps = {
  store: KeyValueStore;
  publisher: SagaPublisher;
};

export function registerAprovacao(app: FastifyInstance, deps: AprovacaoDeps): void {
  app.post('/solicitacoes/:cpf/aprovacao', async (request: FastifyRequest, reply: FastifyReply) => {
    const cpf = (request.params as { cpf: string }).cpf;
    const jobId = randomUUID();
    const accepted = { jobId, status: JobStatus.PENDENTE };
    const job = { ...accepted, cpf: request.user?.cpf };
    await saveJob(deps.store, job);
    try {
      await deps.publisher.publish({
        sagaId: jobId,
        tipo: CommandTypes.APROVAR_CLIENTE,
        timestamp: nowTimestamp(),
        payload: { cpf, solicitadoPorCpf: request.user?.cpf },
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
