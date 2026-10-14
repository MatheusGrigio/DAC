export type JobStatus = 'PENDENTE' | 'CONCLUIDO' | 'FALHA';

export type JobResultType = 'resource' | 'inline';

export interface JobResponse<T = unknown> {
  jobId: string;
  status: JobStatus;
  resultType?: JobResultType | null;
  dominio?: string | null;
  resourceId?: string | null;
  resultado?: T | null;
  erro?: string | null;
}
