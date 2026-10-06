import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth.service';

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
  imports: [CommonModule, RouterModule],
  templateUrl: './dashboard.html',
  styleUrls: ['./dashboard.css']
})
export class DashboardClienteComponent {
  private authService = inject(AuthService);
  
  get usuario() {
    return this.authService.usuarioLogado();
  }
  
  logout(): void {
    this.authService.logout();
  }
}
