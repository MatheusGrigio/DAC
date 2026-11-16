import { Component, inject, viewChild } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { Button } from '../../shared/components/button/button';
import { Confirmacao } from '../../shared/components/confirmacao/confirmacao';

@Component({
  selector: 'app-gerente-area',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, Button, Confirmacao],
  templateUrl: './area.html',
  styleUrl: './area.css',
})
export class GerenteArea {
  private authService = inject(AuthService);
  private readonly confirmacao = viewChild.required(Confirmacao);

  readonly usuario = this.authService.usuarioLogado;

  readonly navItens = [
    { path: 'solicitacoes', label: 'Solicitações de autocadastro' },
    { path: 'clientes', label: 'Consultar clientes' },
    { path: 'relatorio', label: 'Relatório de clientes' },
    { path: 'gerentes', label: 'Gerentes' },
  ] as const;

  logout(): void {
    this.confirmacao().abrir({
      titulo: 'Sair do BANTADS',
      mensagem: 'Deseja encerrar sua sessão?',
      rotulo: 'Sair',
    }, () => this.authService.logout());
  }
}
