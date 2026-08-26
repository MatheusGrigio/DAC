export class Login {
    static readonly Request: LoginRequest;
    static readonly Response: LoginResponse;
}

export interface LoginRequest {
  login: string;
  senha: string;
}

export interface LoginResponse {
  auth: boolean;
  token: string;
  tipo: 'CLIENTE' | 'GERENTE';
  usuario: {
    cpf: string;
    nome: string;
    email: string;
  };
}

