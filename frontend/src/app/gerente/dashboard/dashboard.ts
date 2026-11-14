import { Component, DestroyRef, inject, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { switchMap } from 'rxjs/operators';
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

  solicitacoes: Solicitacao[] = [];
  loading = false;
  processandoCpf: string | null = null;
  error = '';
  mensagemSucesso = '';
  detalheCpf: string | null = null;
  detalhe: Solicitacao | null = null;
  carregandoDetalhe = false;

  get usuario() {
    return this.authService.usuarioLogado();
  }

  ngOnInit() {
    this.carregarSolicitacoes();
  }

  carregarSolicitacoes() {
    this.loading = true;
    this.error = '';
    this.solicitacaoService.listar().subscribe({
      next: (res) => {
        this.solicitacoes = res.solicitacoes || [];
        this.loading = false;
      },
      error: () => {
        this.error = 'Falha ao carregar solicitações.';
        this.loading = false;
      },
    });
  }

  verDetalhes(cpf: string) {
    if (this.detalheCpf === cpf && this.detalhe) {
      this.detalheCpf = null;
      this.detalhe = null;
      return;
    }
    this.carregandoDetalhe = true;
    this.detalheCpf = cpf;
    this.detalhe = null;
    this.solicitacaoService
      .obter(cpf)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (s) => {
          this.detalhe = s;
          this.carregandoDetalhe = false;
        },
        error: () => {
          this.error = 'Não foi possível carregar os detalhes da solicitação.';
          this.detalheCpf = null;
          this.carregandoDetalhe = false;
        },
      });
  }

  aprovar(cpf: string) {
    if (!confirm('Deseja realmente aprovar este cliente?')) return;
    this.processandoCpf = cpf;
    this.error = '';
    this.mensagemSucesso = '';

    this.solicitacaoService
      .aprovar(cpf)
      .pipe(
        switchMap((res) => this.jobService.aguardarConclusao(res.jobId)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.processandoCpf = null;
          this.mensagemSucesso = 'Cliente aprovado com sucesso!';
          this.carregarSolicitacoes();
        },
        error: (err) => {
          this.processandoCpf = null;
          this.error =
            err.name === 'TimeoutError'
              ? 'A aprovação demorou para concluir. Verifique a lista e tente novamente.'
              : mensagemDeErro(err, 'Erro ao processar aprovação do cliente.');
          this.carregarSolicitacoes();
        },
      });
  }

  rejeitar(cpf: string) {
    const motivo = prompt('Informe o motivo da rejeição:');
    if (motivo && motivo.trim()) {
      this.processandoCpf = cpf;
      this.error = '';
      this.mensagemSucesso = '';

      this.solicitacaoService
        .rejeitar(cpf, motivo.trim())
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: () => {
            this.processandoCpf = null;
            this.mensagemSucesso = 'Cliente rejeitado com sucesso.';
            this.carregarSolicitacoes();
          },
          error: (err) => {
            this.processandoCpf = null;
            this.error = mensagemDeErro(err, 'Erro ao rejeitar cliente.');
          },
        });
    }
  }

  logout(): void {
    this.authService.logout();
  }
}