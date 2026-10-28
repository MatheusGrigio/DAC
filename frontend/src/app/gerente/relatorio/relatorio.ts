import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { finalize, of, switchMap } from 'rxjs';
import { Button } from '../../shared/components/button/button';
import { Alert } from '../../shared/components/alert/alert';
import { environment } from '../../../environments/environment';
import { JobResponse } from '../../shared/models/job.model';
import { RelatorioClienteLinha, RelatorioClientes } from '../../shared/models/relatorio.model';
import { CpfPipe, MoedaPipe } from '../../shared/pipes';
import { mensagemDeErro } from '../../shared/utils/erro-http';
import { JobService } from '../../services/job.service';

@Component({
  selector: 'app-relatorio',
  imports: [RouterLink, Button, Alert, CpfPipe, MoedaPipe],
  templateUrl: './relatorio.html',
  styleUrl: './relatorio.css',
})
export class Relatorio implements OnInit {
  private http = inject(HttpClient);
  private jobService = inject(JobService);
  private destroyRef = inject(DestroyRef);
  private readonly apiUrl = environment.apiUrl;

  clientes = signal<RelatorioClienteLinha[]>([]);
  carregando = signal(false);
  erro = signal('');

  ngOnInit(): void {
    this.carregar();
  }

  carregar(): void {
    if (this.carregando()) return;
    this.carregando.set(true);
    this.erro.set('');
    this.clientes.set([]);

    this.http
      .get<RelatorioClientes | JobResponse>(`${this.apiUrl}/relatorios/clientes`, {
        observe: 'response',
      })
      .pipe(
        switchMap((response) => {
          if (response.status !== 202) return of(response.body as RelatorioClientes);
          const job = response.body as JobResponse;
          return this.jobService
            .aguardarConclusao(job.jobId)
            .pipe(switchMap(() => this.jobService.obterResultado<RelatorioClientes>(job.jobId)));
        }),
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.carregando.set(false)),
      )
      .subscribe({
        next: (response) =>
          this.clientes.set(
            [...response.clientes].sort((a, b) => a.nome.localeCompare(b.nome, 'pt-BR')),
          ),
        error: (error) =>
          this.erro.set(
            error.name === 'TimeoutError'
              ? 'O relatório demorou para responder. Tente novamente.'
              : mensagemDeErro(error, 'Não foi possível carregar o relatório. Tente novamente.'),
          ),
      });
  }
}
