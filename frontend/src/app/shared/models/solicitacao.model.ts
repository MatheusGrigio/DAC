export type StatusSolicitacao = 'PENDENTE' | 'APROVADA' | 'REJEITADA';

export interface Solicitacao {
  cpf: string;
  nome: string;
  email: string;
  telefone: string;
  salario: string;
  status: StatusSolicitacao;
  motivo?: string;
  dataHoraProcessamento?: string;
  _links?: Record<string, { href: string }>;
}

export interface SolicitacoesListModel {
  solicitacoes: Solicitacao[];
  _links?: Record<string, { href: string }>;
}
