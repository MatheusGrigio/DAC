import { Component, inject, OnInit, signal, viewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { switchMap } from 'rxjs/operators';
import { AuthService, SolicitacaoService, JobService } from '../../services';
import { Button } from '../../shared/components/button/button';
import { Alert } from '../../shared/components/alert/alert';
import { Confirmacao } from '../../shared/components/confirmacao/confirmacao';
import { Solicitacao } from '../../shared/models';
import { MoedaPipe } from '../../shared/pipes/moeda.pipe';
import { CpfPipe } from '../../shared/pipes/cpf.pipe';
import { DataHoraPipe } from '../../shared/pipes/data-hora.pipe';

@Component({
  selector: 'app-dashboard-gerente',
  standalone: true,
  imports: [CommonModule, RouterModule, Button, Alert, Confirmacao, MoedaPipe, CpfPipe, DataHoraPipe],
  templateUrl: './dashboard.html',
  styleUrls: ['./dashboard.css'],
})
export class DashboardGerenteComponent implements OnInit {
  private authService = inject(AuthService);
  private solicitacaoService = inject(SolicitacaoService);
  private jobService = inject(JobService);
  private readonly confirmacao = viewChild.required(Confirmacao);

  readonly solicitacoes = signal<Solicitacao[]>([]);
  readonly loading = signal(false);
  readonly processandoCpf = signal<string | null>(null);
  readonly error = signal('');
  readonly mensagemSucesso = signal('');

  get usuario() {
    return this.authService.usuarioLogado();
  }

  ngOnInit() {
    this.carregarSolicitacoes();
  }

  carregarSolicitacoes() {
    this.loading.set(true);
    this.error.set('');
    this.solicitacaoService.listar().subscribe({
      next: (res) => {
        this.solicitacoes.set(res.solicitacoes);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Erro ao listar solicitacoes', err);
        this.error.set('Falha ao carregar solicitações.');
        this.loading.set(false);
      },
    });
  }

  pedirAprovacao(solicitacao: Solicitacao): void {
    this.confirmacao().abrir({
      titulo: 'Aprovar autocadastro',
      mensagem: `Deseja aprovar a solicitação de ${solicitacao.nome}?`,
      rotulo: 'Aprovar',
    }, () => this.aprovar(solicitacao.cpf));
  }

  pedirRejeicao(solicitacao: Solicitacao): void {
    this.confirmacao().abrir({
      titulo: 'Recusar autocadastro',
      mensagem: `Informe o motivo para recusar a solicitação de ${solicitacao.nome}.`,
      rotulo: 'Recusar',
      perigo: true,
      rotuloMotivo: 'Motivo da rejeição',
    }, (motivo) => this.rejeitar(solicitacao.cpf, motivo));
  }

  private aprovar(cpf: string): void {
    this.processandoCpf.set(cpf);
    this.error.set('');
    this.mensagemSucesso.set('');

    this.solicitacaoService
      .aprovar(cpf)
      .pipe(
        switchMap((res) => this.jobService.aguardarConclusao(res.jobId)),
        switchMap((job) => this.jobService.obterRecursoDoJob<{ nome: string }>(job)),
      )
      .subscribe({
        next: (cliente) => {
          this.processandoCpf.set(null);
          this.mensagemSucesso.set(cliente
            ? `Cliente ${cliente.nome} aprovado com sucesso!`
            : 'Cliente aprovado, mas não foi possível carregar seus dados.');
          this.carregarSolicitacoes();
        },
        error: (err) => {
          this.processandoCpf.set(null);
          this.carregarSolicitacoes();
          this.error.set(
            err.mensagem ||
            err.error?.mensagem ||
            'Erro ao processar aprovação do cliente.',
          );
        },
      });
  }

  private rejeitar(cpf: string, motivo: string): void {
    this.processandoCpf.set(cpf);
    this.error.set('');
    this.mensagemSucesso.set('');

    this.solicitacaoService.rejeitar(cpf, motivo).subscribe({
      next: () => {
        this.processandoCpf.set(null);
        this.mensagemSucesso.set('Cliente rejeitado com sucesso.');
        this.carregarSolicitacoes();
      },
      error: (err) => {
        this.processandoCpf.set(null);
        this.error.set(err.error?.mensagem || 'Erro ao rejeitar cliente.');
      },
    });
  }

  logout(): void {
    this.authService.logout();
  }
}
