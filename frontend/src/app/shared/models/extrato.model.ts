import { Links } from './conta.model';

export interface ParteMovimentacao {
  numeroConta: string;
  cpf: string;
  nome: string;
}

export interface MovimentacaoResponse {
  dataHora: string;
  tipo: 'DEPOSITO' | 'SAQUE' | 'TRANSFERENCIA' | string;
  valor: string;
  origem?: ParteMovimentacao | null;
  destino?: ParteMovimentacao | null;
}

export interface ExtratoResponse {
  numeroConta: string;
  dataInicio: string;
  dataFim: string;
  saldoAbertura: string;
  movimentacoes: MovimentacaoResponse[];
  _links?: Links;
}
