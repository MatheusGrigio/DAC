import { JOB_TTL_SECONDS } from '../config.js';
import type { KeyValueStore } from './store.js';

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

export function jobKey(jobId: string): string {
  return `job:${jobId}`;
}

export async function saveJob(
  store: KeyValueStore,
  job: StoredJob,
  ttlSeconds = JOB_TTL_SECONDS,
): Promise<void> {
  await store.set(jobKey(job.jobId), JSON.stringify(job), ttlSeconds);
}

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
