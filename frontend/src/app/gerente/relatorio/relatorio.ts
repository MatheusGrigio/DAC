import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { exhaustMap, filter, finalize, of, switchMap, take, throwError, timeout, timer } from 'rxjs';
import { Button } from '../../shared/components/button/button';
import { Alert } from '../../shared/components/alert/alert';
import { RelatorioClienteLinha, RelatorioClientes } from '../../shared/models/relatorio.model';
import { MoedaPipe } from '../../shared/pipes/moeda.pipe';
import { mensagemDeErro } from '../../shared/utils/erro-http';

interface JobRelatorio {
  jobId: string;
  status: 'PENDENTE' | 'CONCLUIDO' | 'FALHA';
  erro?: string;
}

@Component({
  selector: 'app-relatorio',
  imports: [RouterLink, Button, Alert, MoedaPipe],
  templateUrl: './relatorio.html',
  styleUrl: './relatorio.css',
})
export class Relatorio implements OnInit {
  private http = inject(HttpClient);
  private destroyRef = inject(DestroyRef);
  private apiUrl = 'http://localhost:3000';

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

    this.http.get<RelatorioClientes | JobRelatorio>(`${this.apiUrl}/relatorios/clientes`, {
      observe: 'response',
    }).pipe(
      switchMap(response => {
        if (response.status !== 202) return of(response.body as RelatorioClientes);
        const job = response.body as JobRelatorio;
        return timer(0, 1000).pipe(
          exhaustMap(() => this.http.get<JobRelatorio>(`${this.apiUrl}/jobs/${job.jobId}/status`)),
          filter(status => status.status !== 'PENDENTE'),
          take(1),
          switchMap(status => status.status === 'FALHA'
            ? throwError(() => new Error(status.erro || 'Falha ao gerar relatório.'))
            : this.http.get<RelatorioClientes>(`${this.apiUrl}/jobs/${job.jobId}/result`)),
        );
      }),
      timeout(60000),
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.carregando.set(false)),
    ).subscribe({
      next: response => this.clientes.set(
        [...response.clientes].sort((a, b) => a.nome.localeCompare(b.nome, 'pt-BR'))
      ),
      error: error => this.erro.set(error.name === 'TimeoutError'
        ? 'O relatório demorou para responder. Tente novamente.'
        : mensagemDeErro(error, error.message || 'Não foi possível carregar o relatório.')),
    });
import { RelatorioClienteLinha } from '../../shared/models';
import { CpfPipe, MoedaPipe } from '../../shared/pipes';

@Component({
  selector: 'app-relatorio',
  imports: [RouterLink, Button, Alert, CpfPipe, MoedaPipe],
  templateUrl: './relatorio.html',
  styleUrl: './relatorio.css',
})
export class Relatorio {
  clientes: RelatorioClienteLinha[] = [];

  get clientesOrdenados(): RelatorioClienteLinha[] {
    return [...this.clientes].sort((a, b) => a.nome.localeCompare(b.nome, 'pt-BR'));
  }
}
