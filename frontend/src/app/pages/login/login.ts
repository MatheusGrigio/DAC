import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { LoginRequest } from '../../shared/models';
import { mensagemDeErro } from '../../shared/utils/erro-http';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login implements OnInit {
  credenciais: LoginRequest = {
    email: '',
    senha: '',
  };

  readonly erroLogin = signal('');
  readonly carregando = signal(false);
  mostrarSenha = false;

  private authService = inject(AuthService);
  private router = inject(Router);

  ngOnInit(): void {
    if (this.authService.usuarioLogado()) {
      const tipo = this.authService.getTipo();
      if (tipo) {
        this.router.navigate([tipo === 'CLIENTE' ? '/cliente/dashboard' : '/gerente/solicitacoes']);
      }
    }
  }

  fazerLogin(): void {
    this.erroLogin.set('');

    if (!this.credenciais.email || !this.credenciais.senha) {
      this.erroLogin.set('Por favor, preencha todos os campos.');
      return;
    }

    this.carregando.set(true);
    this.authService
      .login(this.credenciais)
      .pipe(finalize(() => this.carregando.set(false)))
      .subscribe({
        next: (response) => {
          this.authService.usuarioLogado.set(response.usuario);
          this.authService.tipoLogado.set(response.tipo);
          if (response.tipo === 'CLIENTE') {
            this.router.navigate(['/cliente/dashboard']);
          } else {
            this.router.navigate(['/gerente/solicitacoes']);
          }
        },
        error: (error: { mensagem?: string }) => {
          this.erroLogin.set(
            error.mensagem ?? mensagemDeErro(error, 'E-mail ou senha inválidos.'),
          );
        },
      });
  }
}