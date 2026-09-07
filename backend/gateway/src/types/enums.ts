export const SagaType = {
  APROVAR_CLIENTE: 'aprovar-cliente',
  INSERIR_GERENTE: 'inserir-gerente',
  REMOVER_GERENTE: 'remover-gerente',
} as const;

export const EventType = {
  CRIADO: 'Criado',
  SAQUE: 'Saque',
  DEPOSITO: 'Depósito',
  TRANSFERENCIA_ORIGEM: 'TransferênciaOrigem',
  TRANSFERENCIA_DESTINO: 'TransferênciaDestino',
  GERENTE_ALTERADO: 'GerenteAlterado',
} as const;

export const JobStatus = {
  PENDENTE: 'PENDENTE',
  CONCLUIDO: 'CONCLUIDO',
  FALHA: 'FALHA',
} as const;

export const ResultType = {
  RESOURCE: 'resource',
  INLINE: 'inline',
} as const;

export const ReplyStatus = {
  SUCESSO: 'SUCESSO',
  FALHA: 'FALHA',
} as const;

export const Perfil = {
  CLIENTE: 'CLIENTE',
  GERENTE: 'GERENTE',
} as const;

export const AuthMessages = {
  TOKEN_AUSENTE: 'Token não fornecido.',
  TOKEN_INVALIDO: 'Falha ao autenticar o token.',
  LOGIN_INVALIDO: 'Login inválido!',
} as const;

export type SagaTypeName = (typeof SagaType)[keyof typeof SagaType];
export type EventTypeName = (typeof EventType)[keyof typeof EventType];
export type JobStatusName = (typeof JobStatus)[keyof typeof JobStatus];
export type ResultTypeName = (typeof ResultType)[keyof typeof ResultType];
export type ReplyStatusName = (typeof ReplyStatus)[keyof typeof ReplyStatus];
export type PerfilName = (typeof Perfil)[keyof typeof Perfil];
