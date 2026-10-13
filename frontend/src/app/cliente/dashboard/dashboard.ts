import { Component, computed, inject, signal } from '@angular/core';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ContaService } from '../../services';
import { mensagemDeErro } from '../../shared/utils/erro-http';

interface Operacao {
  rel: string; 
  rota: string;
  titulo: string;
  descricao: string;
  disponivel: boolean;
}

const OPERACOES: Operacao[] = [
  { rel: 'deposito', rota: '/cliente/deposito', titulo: 'Depósito', descricao: 'Adicionar dinheiro à conta', disponivel: true },
  { rel: 'saque', rota: '/cliente/saque', titulo: 'Saque', descricao: 'Retirar dinheiro da conta', disponivel: true },
  { rel: 'transferencia', rota: '/cliente/transferencia', titulo: 'Transferência', descricao: 'Enviar para outra conta', disponivel: true },
  { rel: 'extrato', rota: '/cliente/extrato', titulo: 'Extrato', descricao: 'Consultar movimentações', disponivel: false },
];

@Component({
  selector: 'app-dashboard-cliente',
  standalone: true,
  imports: [RouterModule],
  templateUrl: './dashboard.html',
  styleUrls: ['./dashboard.css']
})
export class DashboardClienteComponent {
  private authService = inject(AuthService);
  private contaService = inject(ContaService);

  protected readonly usuario = this.authService.usuarioLogado;
  protected readonly conta = this.contaService.conta;
  protected readonly carregando = signal(true);
  protected readonly erro = signal('');

  protected readonly primeiroNome = computed(
    () => this.usuario()?.nome.split(' ')[0] || '',
  )

  protected readonly operacoes = computed(() => 
    OPERACOES.filter((op) =>  !!this.conta()?.links[op.rel]),
  );

  ngOnInit(): void {
    this.carregar();
  }

  protected carregar(): void {
    const cpf = this.usuario()?.cpf;
    if (!cpf) {
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
    this.authService.logout();
  }
}
