import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { Button } from '../../shared/components/button/button';

@Component({
  selector: 'app-gerente-area',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, Button],
  templateUrl: './area.html',
  styleUrl: './area.css',
})
export class GerenteArea {
  private authService = inject(AuthService);

  readonly usuario = this.authService.usuarioLogado;

  readonly navItens = [
    { path: 'solicitacoes', label: 'Solicitações de autocadastro' },
    { path: 'clientes', label: 'Consultar clientes' },
    { path: 'relatorio', label: 'Relatório de clientes' },
    { path: 'gerentes', label: 'Gerentes' },
  ] as const;

  logout(): void {
    if (!confirm('Tem certeza que deseja sair do aplicativo?')) return;
    this.authService.logout();
  }
}