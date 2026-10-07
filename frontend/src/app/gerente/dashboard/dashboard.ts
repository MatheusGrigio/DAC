import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService, SolicitacaoService } from '../../services';
import { Button } from '../../shared/components/button/button';
import { Solicitacao } from '../../shared/models';

@Component({
  selector: 'app-dashboard-gerente',
  standalone: true,
  imports: [CommonModule, RouterModule, Button],
  templateUrl: './dashboard.html',
  styleUrls: ['./dashboard.css']
})
export class DashboardGerenteComponent implements OnInit {
  private authService = inject(AuthService);
  private solicitacaoService = inject(SolicitacaoService);

  solicitacoes: Solicitacao[] = [];
  loading = false;
  error = '';
  
  get usuario() {
    return this.authService.usuarioLogado();
  }
  
  ngOnInit() {
    this.carregarSolicitacoes();
  }

  carregarSolicitacoes() {
    this.loading = true;
    this.solicitacaoService.listar().subscribe({
      next: (res) => {
        this.solicitacoes = res.solicitacoes || [];
        this.loading = false;
      },
      error: (err) => {
        console.error('Erro ao listar solicitacoes', err);
        this.error = 'Falha ao carregar solicitações.';
        this.loading = false;
      }
    });
  }

  aprovar(cpf: string) {
    if (!confirm('Deseja realmente aprovar este cliente?')) return;
    this.solicitacaoService.aprovar(cpf).subscribe({
      next: () => {
        alert('Aprovação iniciada com sucesso (job assíncrono).');
        this.carregarSolicitacoes();
      },
      error: () => alert('Erro ao aprovar cliente')
    });
  }

  rejeitar(cpf: string) {
    const motivo = prompt('Informe o motivo da rejeição:');
    if (motivo) {
      this.solicitacaoService.rejeitar(cpf, motivo).subscribe({
        next: () => {
          alert('Cliente rejeitado.');
          this.carregarSolicitacoes();
        },
        error: () => alert('Erro ao rejeitar cliente')
      });
    }
  }

  logout(): void {
    this.authService.logout();
  }
}
