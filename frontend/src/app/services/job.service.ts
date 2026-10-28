import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, timer, throwError, of } from 'rxjs';
import { exhaustMap, filter, switchMap, take, timeout } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { JobResponse } from '../shared/models/job.model';

@Injectable({
  providedIn: 'root',
})
export class JobService {
  private http = inject(HttpClient);
  private apiUrl = environment.apiUrl;

  consultarStatus<T = unknown>(jobId: string): Observable<JobResponse<T>> {
    return this.http.get<JobResponse<T>>(`${this.apiUrl}/jobs/${jobId}/status`);
  }

  obterResultado<T>(jobId: string): Observable<T> {
    return this.http.get<T>(`${this.apiUrl}/jobs/${jobId}/result`);
  }

  obterRecurso<T>(dominio: string, resourceId: string): Observable<T> {
    return this.http.get<T>(`${this.apiUrl}/${dominio}/${resourceId}`);
  }

  aguardarConclusao<T = unknown>(
    jobId: string,
    intervaloMs = 1000,
    tempoLimiteMs = 60000,
  ): Observable<JobResponse<T>> {
    return timer(0, intervaloMs).pipe(
      exhaustMap(() => this.consultarStatus<T>(jobId)),
      filter((job) => job.status !== 'PENDENTE'),
      take(1),
      switchMap((job) => {
        if (job.status === 'FALHA') {
          const mensagem = job.erro || 'Falha durante o processamento da operação.';
          return throwError(() => ({
            name: 'JobError',
            status: 'FALHA',
            message: mensagem,
            error: { mensagem },
            job,
          }));
        }
        return of(job);
      }),
      timeout(tempoLimiteMs),
    );
  }
}
