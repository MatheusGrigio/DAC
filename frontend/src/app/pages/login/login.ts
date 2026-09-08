import { CommonModule } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core'; 
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { LoginRequest } from '../../shared/models';

@Component({
  selector: 'app-login',
  standalone: true,  
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login implements OnInit {  
  credenciais: LoginRequest = {
    login: '',
    senha: '',
  };
  
  erroLogin: string = '';
  carregando: boolean = false;

  private authService = inject(AuthService);
  private router = inject(Router);

  ngOnInit(): void {
    if (this.authService.usuarioLogado()) {
      const tipo = this.authService.getTipo();
      if (tipo) {
        this.router.navigate([tipo === 'CLIENTE' ? '/cliente/dashboard' : '/gerente/dashboard']);
      }
    }
  }

  fazerLogin(): void {
    this.erroLogin = '';

    if (!this.credenciais.login || !this.credenciais.senha) {
      this.erroLogin = 'Por favor, preencha todos os campos.';
      return;
    }

    this.carregando = true;

    this.authService.login(this.credenciais).subscribe({
      next: (response) => {
        this.carregando = false;
        this.authService.usuarioLogado.set(response.usuario);
        this.authService.tipoLogado.set(response.tipo);
        if (response.tipo === 'CLIENTE') {
          this.router.navigate(['/cliente/dashboard']);
        } else {
          this.router.navigate(['/gerente/dashboard']);
        }
      },
      error: (error) => {
        this.carregando = false;
        this.erroLogin = error.mensagem || 'Erro ao fazer login.';
      }
    });
  }
}