import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { finalize, switchMap } from 'rxjs';
import { AuthService, SolicitacaoService, JobService } from '../../services';
import { Button } from '../../shared/components/button/button';
import { Alert } from '../../shared/components/alert/alert';
import { Solicitacao } from '../../shared/models';
import { MoedaPipe } from '../../shared/pipes/moeda.pipe';
import { CpfPipe } from '../../shared/pipes/cpf.pipe';
import { DataHoraPipe } from '../../shared/pipes/data-hora.pipe';
import { mensagemDeErro } from '../../shared/utils/erro-http';

@Component({
  selector: 'app-dashboard-gerente',
  standalone: true,
  imports: [CommonModule, RouterModule, Button, Alert, MoedaPipe, CpfPipe, DataHoraPipe],
  templateUrl: './dashboard.html',
  styleUrls: ['./dashboard.css'],
})
export class DashboardGerenteComponent implements OnInit {
  private authService = inject(AuthService);
  private solicitacaoService = inject(SolicitacaoService);
  private jobService = inject(JobService);
  private destroyRef = inject(DestroyRef);

  readonly solicitacoes = signal<Solicitacao[]>([]);
  readonly loading = signal(false);
  readonly processandoCpf = signal<string | null>(null);
  readonly error = signal('');
  readonly mensagemSucesso = signal('');
  readonly detalheCpf = signal<string | null>(null);
  readonly detalhe = signal<Solicitacao | null>(null);
  readonly carregandoDetalhe = signal(false);

  get usuario() {
    return this.authService.usuarioLogado();
  }

  ngOnInit() {
    queueMicrotask(() => this.carregarSolicitacoes());
  }

  carregarSolicitacoes() {
    if (this.loading()) return;
    this.loading.set(true);
    this.error.set('');
    this.solicitacaoService
      .listar()
      .pipe(
        finalize(() => this.loading.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (res) => {
          this.solicitacoes.set(res.solicitacoes ?? []);
        },
        error: () => {
          this.error.set('Falha ao carregar solicitações.');
        },
      });
  }

  verDetalhes(cpf: string) {
    if (this.detalheCpf() === cpf && this.detalhe()) {
      this.detalheCpf.set(null);
      this.detalhe.set(null);
      return;
    }
    this.carregandoDetalhe.set(true);
    this.detalheCpf.set(cpf);
    this.detalhe.set(null);
    this.solicitacaoService
      .obter(cpf)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (s) => {
          this.detalhe.set(s);
          this.carregandoDetalhe.set(false);
        },
        error: () => {
          this.error.set('Não foi possível carregar os detalhes da solicitação.');
          this.detalheCpf.set(null);
          this.carregandoDetalhe.set(false);
        },
      });
  }

  aprovar(cpf: string) {
    if (!confirm('Deseja realmente aprovar este cliente?')) return;
    this.processandoCpf.set(cpf);
    this.error.set('');
    this.mensagemSucesso.set('');

    this.solicitacaoService
      .aprovar(cpf)
      .pipe(
        switchMap((res) => this.jobService.aguardarConclusao(res.jobId)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.processandoCpf.set(null);
          this.mensagemSucesso.set('Cliente aprovado com sucesso!');
          this.carregarSolicitacoes();
        },
        error: (err) => {
          this.processandoCpf.set(null);
          this.error.set(
            err.name === 'TimeoutError'
              ? 'A aprovação demorou para concluir. Verifique a lista e tente novamente.'
              : mensagemDeErro(err, 'Erro ao processar aprovação do cliente.'),
          );
          this.carregarSolicitacoes();
        },
      });
  }

  rejeitar(cpf: string) {
    const motivo = prompt('Informe o motivo da rejeição:');
    if (motivo && motivo.trim()) {
      this.processandoCpf.set(cpf);
      this.error.set('');
      this.mensagemSucesso.set('');

      this.solicitacaoService
        .rejeitar(cpf, motivo.trim())
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: () => {
            this.processandoCpf.set(null);
            this.mensagemSucesso.set('Cliente rejeitado com sucesso.');
            this.carregarSolicitacoes();
          },
          error: (err) => {
            this.processandoCpf.set(null);
            this.error.set(mensagemDeErro(err, 'Erro ao rejeitar cliente.'));
          },
        });
    }
  }

  logout(): void {
    if (!confirm('Tem certeza que deseja sair do aplicativo?')) return;
    this.authService.logout();
  }
}
