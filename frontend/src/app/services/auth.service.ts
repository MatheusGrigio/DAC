import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, tap, catchError, throwError } from 'rxjs';
import { Router } from '@angular/router';

export interface LoginRequest {
  login: string;
  senha: string;
}

export interface UsuarioResponse {
  cpf: string;
  nome: string;
  email: string;
}

export type TipoUsuario = 'CLIENTE' | 'GERENTE';

export interface LoginResponse {
  auth: boolean;      
  token: string;     
  tipo: 'CLIENTE' | 'GERENTE';  
  usuario: UsuarioResponse;
}

@Injectable({
  providedIn: 'root',
})
export class Auth {
  private readonly API_GATEWAY = 'http://localhost:3000';
  private readonly TOKEN_KEY = 'token';
  private readonly USUARIO_KEY = 'usuario';
  private readonly TIPO_KEY = 'tipo';

  private http = inject(HttpClient);   
  private router = inject(Router);  

  readonly usuarioLogado = signal<UsuarioResponse | null>(this.lerUsuarioSalvo());
  readonly tipoLogado = signal<TipoUsuario | null>(
    (localStorage.getItem(this.TIPO_KEY) as TipoUsuario | null) ?? null,
);

  login(credenciais: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.API_GATEWAY}/login`, credenciais).pipe(
      tap((response: LoginResponse) => {
        if (response.auth) {
          localStorage.setItem(this.TOKEN_KEY, response.token);
          localStorage.setItem(this.USUARIO_KEY, JSON.stringify(response.usuario));
          localStorage.setItem(this.TIPO_KEY, response.tipo);
        }
      }), 
      catchError(error => {
        let mensagem = 'Erro ao fazer login. Tente novamente.';
        if (error.status === 401) {
        mensagem = 'E-mail ou senha inválidos.';
        } else if (error.status === 403) {
        mensagem = 'Usuário inativo ou bloqueado.';
        }
        return throwError(() => ({ mensagem, status: error.status }));
      })
    );
}

  logout(): void {
    this.http.post(`${this.API_GATEWAY}/logout`, {}).subscribe({
      complete: () => this.encerrarSessaoLocal(),
      error: () => this.encerrarSessaoLocal(),
    });
  }
 
  encerrarSessaoLocal(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.USUARIO_KEY);
    localStorage.removeItem(this.TIPO_KEY);
    this.usuarioLogado.set(null);
    this.tipoLogado.set(null);
    this.router.navigate(['/login']);
  }
 
  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }
 
  getTipo(): TipoUsuario | null {
    return this.tipoLogado();
  }
 
  isAuthenticated(): boolean {
    return !!this.getToken();
  }
 
  private lerUsuarioSalvo(): UsuarioResponse | null {
    const bruto = localStorage.getItem(this.USUARIO_KEY);
    return bruto ? JSON.parse(bruto) : null;
  }
}