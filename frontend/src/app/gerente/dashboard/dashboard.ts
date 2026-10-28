import { Component, inject, OnInit } from '@angular/core';
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

  solicitacoes: Solicitacao[] = [];
  loading = false;
  processandoCpf: string | null = null;
  error = '';
  mensagemSucesso = '';

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
      error: (err) => {
        console.error('Erro ao listar solicitacoes', err);
        this.error = 'Falha ao carregar solicitações.';
        this.loading = false;
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
      .pipe(switchMap((res) => this.jobService.aguardarConclusao(res.jobId)))
      .subscribe({
        next: () => {
          this.processandoCpf = null;
          this.mensagemSucesso = 'Cliente aprovado com sucesso!';
          this.carregarSolicitacoes();
        },
        error: (err) => {
          this.processandoCpf = null;
          this.error =
            err.mensagem ||
            err.error?.mensagem ||
            'Erro ao processar aprovação do cliente.';
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

      this.solicitacaoService.rejeitar(cpf, motivo.trim()).subscribe({
        next: () => {
          this.processandoCpf = null;
          this.mensagemSucesso = 'Cliente rejeitado com sucesso.';
          this.carregarSolicitacoes();
        },
        error: (err) => {
          this.processandoCpf = null;
          this.error = err.error?.mensagem || 'Erro ao rejeitar cliente.';
        },
      });
    }
  }

  logout(): void {
    this.authService.logout();
  }
}
