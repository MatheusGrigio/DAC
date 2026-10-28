import { Decimal } from 'decimal.js';
import { DateTime } from 'luxon';
import { Links } from './conta.model';

export type StatusSolicitacao = 'PENDENTE' | 'APROVADA' | 'NAO_APROVADA' | 'REJEITADA';

export interface Endereco {
  logradouro: string;
  numero: string;
  complemento?: string | null;
  cep: string;
  cidade: string;
  uf: string;
}

export interface AutocadastroRequest {
  cpf: string;
  nome: string;
  email: string;
  telefone: string;
  salario: string;
  endereco: Endereco;
}

export interface SolicitacaoResponse {
  cpf: string;
  nome: string;
  email: string;
  telefone: string;
  salario: string;
  endereco: Endereco;
  status: StatusSolicitacao;
  motivo: string | null;
  dataHoraProcessamento: string | null;
  _links?: Links;
}

export interface SolicitacaoListaResponse {
  solicitacoes: SolicitacaoResponse[];
}

export interface Solicitacao {
  cpf: string;
  nome: string;
  email: string;
  telefone: string;
  salario: Decimal;
  status: StatusSolicitacao;
  motivo?: string;
  dataHoraProcessamento: DateTime | null;
  _links?: Links;
}

export interface SolicitacoesListModel {
  solicitacoes: Solicitacao[];
  _links?: Links;
}
