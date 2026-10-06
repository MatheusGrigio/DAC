import { Decimal } from 'decimal.js';
import { DateTime } from 'luxon';

export interface Link {
  href: string;
}

export type Links = Record<string, Link>;

export interface ContaResponse {
  numero: string;
  cpfCliente: string;
  cpfGerente: string;
  saldo: string;
  dataCriacao: string;
  _links: Links;
}

export interface Conta {
  numero: string;
  cpfCliente: string;
  cpfGerente: string;
  saldo: Decimal;
  dataCriacao: DateTime;
  links: Links;
}

export interface OperacaoResponse {
  numeroConta: string;
  tipo: string;
  dataHora: string;
  valor: string;
  _links?: Links;
}
