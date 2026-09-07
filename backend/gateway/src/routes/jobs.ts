import type { FastifyInstance, FastifyReply, FastifyRequest } from 'fastify';
import { Erros } from '../http/errors.js';
import { jobStatusBody, readJob } from '../redis/jobs.js';
import type { KeyValueStore } from '../redis/store.js';
import { JobStatus, ResultType } from '../types/enums.js';

export function registerJobs(app: FastifyInstance, store: KeyValueStore): void {
  app.get('/jobs/:jobId/status', async (request: FastifyRequest, reply: FastifyReply) => {
    const jobId = (request.params as { jobId: string }).jobId;
    const job = await readJob(store, jobId);
    if (!job) {
      return reply.code(404).send(Erros.notFound('Job inexistente ou expirado'));
    }
    if (!isJobOwner(job, request.user?.cpf)) {
      return reply.code(403).send(Erros.forbidden('Job não pertence ao usuário autenticado'));
    }
    return reply.code(200).send(jobStatusBody(job));
  });

  app.get('/jobs/:jobId/result', async (request: FastifyRequest, reply: FastifyReply) => {
    const jobId = (request.params as { jobId: string }).jobId;
    const job = await readJob(store, jobId);
    if (!job) {
      return reply.code(404).send(Erros.notFound('Job inexistente ou expirado'));
    }
    if (!isJobOwner(job, request.user?.cpf)) {
      return reply.code(403).send(Erros.forbidden('Job não pertence ao usuário autenticado'));
    }
    if (job.status !== JobStatus.CONCLUIDO || job.resultType !== ResultType.INLINE) {
      return reply
        .code(409)
        .send(Erros.conflict('Job ainda não concluído, falhou ou não é inline'));
    }
    return reply.code(200).send(job.resultado ?? {});
  });
}

function isJobOwner(job: { cpf?: string | null }, cpf: string | undefined): boolean {
  if (!job.cpf) {
    return true;
  }
  return Boolean(cpf) && job.cpf === cpf;
}
