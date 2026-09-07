import { randomUUID } from 'node:crypto';
import type { FastifyInstance, FastifyReply, FastifyRequest } from 'fastify';
import type { SagaPublisher } from '../amqp/publisher.js';
import { Erros } from '../http/errors.js';
import { nowTimestamp } from '../http/dates.js';
import { saveJob } from '../redis/jobs.js';
import type { KeyValueStore } from '../redis/store.js';
import { CommandTypes } from '../types/command-types.js';
import { JobStatus } from '../types/enums.js';
import { gerenteInputSchema } from '../types/schemas.js';

type InserirDeps = {
  store: KeyValueStore;
  publisher: SagaPublisher;
};

export function registerInserirGerente(app: FastifyInstance, deps: InserirDeps): void {
  app.post('/gerentes', async (request: FastifyRequest, reply: FastifyReply) => {
    const parsed = gerenteInputSchema.safeParse(request.body);
    if (!parsed.success) {
      return reply.code(400).send(Erros.badRequest('Requisição malformada'));
    }
    const jobId = randomUUID();
    const accepted = { jobId, status: JobStatus.PENDENTE };
    const job = { ...accepted, cpf: request.user?.cpf };
    await saveJob(deps.store, job);
    try {
      await deps.publisher.publish({
        sagaId: jobId,
        tipo: CommandTypes.INSERIR_GERENTE,
        timestamp: nowTimestamp(),
        payload: { ...parsed.data },
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
