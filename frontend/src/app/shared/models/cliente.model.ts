import { Decimal } from 'decimal.js';
import { Links } from './conta.model';

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
