import { Decimal } from 'decimal.js';
import { Links } from './conta.model';
import { Endereco } from './solicitacao.model';

export interface ClienteListaItemResponse {
  cpf: string;
  nome: string;
  cidade: string;
  estado: string;
  saldo: string;
  _links?: Links;
}

export interface ClienteListaResponse {
  clientes: ClienteListaItemResponse[];
}

export interface ClienteLista {
  cpf: string;
  nome: string;
  cidade: string;
  estado: string;
  saldo: Decimal;
}

export interface ClienteDetalheResponse {
  cpf: string;
  nome: string;
  email: string;
  telefone: string;
  salario: string;
  endereco: Endereco;
  _links?: Links;
}

export interface ClienteDetalhe {
  cpf: string;
  nome: string;
  email: string;
  telefone: string;
  salario: Decimal;
  endereco: Endereco;
  links: Links;
}
