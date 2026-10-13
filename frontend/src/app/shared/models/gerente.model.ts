import { Links } from './conta.model';

export interface GerenteResponse {
  cpf: string;
  nome: string;
  email: string;
  telefone: string;
  ativo: boolean;
  quantidadeClientes?: number;
  _links?: Links;
}

export interface GerenteListaResponse {
  gerentes: GerenteResponse[];
  _links?: Links;
}

export interface GerenteInput {
  cpf: string;
  nome: string;
  email: string;
  telefone: string;
  senha: string;
}

export interface GerenteAtualizacao {
  nome: string;
  telefone: string;
}

export interface Gerente {
  cpf: string;
  nome: string;
  email: string;
  telefone: string;
  ativo: boolean;
  quantidadeClientes: number;
  links: Links;
}
