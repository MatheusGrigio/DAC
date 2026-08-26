import { UsuarioResponse, LoginRequest } from "../../services";

export class Usuario {
    static readonly Request: LoginRequest;
    static readonly Response: UsuarioResponse;
}

export interface Usuario {
        cpf: string;
        nome: string;
        email: string;
        tipo: 'CLIENTE' | 'GERENTE';
}

export interface UsuarioAutenticado extends Usuario {
  token: string;
  expiraEm: string;
}

