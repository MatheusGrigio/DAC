import { Component, computed, inject, signal, viewChild } from '@angular/core';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ContaService } from '../../services';
import { mensagemDeErro } from '../../shared/utils/erro-http';
import { MoedaPipe } from '../../shared/pipes/moeda.pipe';
import { Alert } from '../../shared/components/alert/alert';
import { Button } from '../../shared/components/button/button';
import { Confirmacao } from '../../shared/components/confirmacao/confirmacao';

interface Operacao {
  rel: string; 
  rota: string;
  titulo: string;
  descricao: string;
}

const OPERACOES: Operacao[] = [
  { rel: 'deposito', rota: '/cliente/deposito', titulo: 'Depósito', descricao: 'Adicionar dinheiro à conta' },
  { rel: 'saque', rota: '/cliente/saque', titulo: 'Saque', descricao: 'Retirar dinheiro da conta' },
  { rel: 'transferencia', rota: '/cliente/transferencia', titulo: 'Transferência', descricao: 'Enviar para outra conta' },
  { rel: 'extrato', rota: '/cliente/extrato', titulo: 'Extrato', descricao: 'Consultar movimentações' },
];

@Component({
  selector: 'app-dashboard-cliente',
  standalone: true,
  imports: [RouterModule, MoedaPipe, Alert, Button, Confirmacao],
  templateUrl: './dashboard.html',
})
export class DashboardClienteComponent {
  private authService = inject(AuthService);
  private contaService = inject(ContaService);
  private readonly confirmacao = viewChild.required(Confirmacao);

  protected readonly usuario = this.authService.usuarioLogado;
  protected readonly conta = this.contaService.conta;
  protected readonly carregando = signal(true);
  protected readonly erro = signal('');

  protected readonly operacoes = computed(() => 
    OPERACOES.filter((op) =>  !!this.conta()?.links[op.rel]),
  );

  ngOnInit(): void {
    this.carregar();
  }

  protected carregar(): void {
    const cpf = this.usuario()?.cpf;
    if (!cpf) {
      this.carregando.set(false);
      this.erro.set('Sua sessão não contém os dados do cliente. Entre novamente.');
      return;
    }
    this.carregando.set(true);
    this.erro.set('');

    this.contaService.carregar(cpf).subscribe({
      next: () => this.carregando.set(false),
      error: (e) => {
        this.carregando.set(false);
        this.erro.set(mensagemDeErro(e, 'Não foi possível carregar sua conta.'));
      },
    });
  }

  logout(): void {
    this.confirmacao().abrir({
      titulo: 'Sair do BANTADS',
      mensagem: 'Deseja encerrar sua sessão?',
      rotulo: 'Sair',
    }, () => this.authService.logout());
  }
}
