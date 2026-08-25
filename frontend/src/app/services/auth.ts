import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
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
  private readonly USUARIO_KEY = 'usuarioLogado';

  private http = inject(HttpClient);
  private router = inject(Router);
}