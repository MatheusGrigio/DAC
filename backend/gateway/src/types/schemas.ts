import { z } from 'zod';
import {
  AuthMessages,
  EventType,
  JobStatus,
  Perfil,
  ReplyStatus,
  ResultType,
  SagaType,
} from './enums.js';
import { CONTA_PATTERN, CPF_PATTERN, DINHEIRO_PATTERN, TIMESTAMP_PATTERN } from './patterns.js';

export const dinheiroSchema = z.string().regex(DINHEIRO_PATTERN);
export const cpfSchema = z.string().regex(CPF_PATTERN);
export const contaSchema = z.string().regex(CONTA_PATTERN);
export const timestampSchema = z.string().regex(TIMESTAMP_PATTERN);

export const loginInputSchema = z
  .object({
    email: z.string().email(),
    senha: z.string().min(1),
  })
  .strict();

export const valorOperacaoSchema = z
  .object({
    valor: dinheiroSchema,
  })
  .strict();

export const transferenciaInputSchema = z
  .object({
    contaDestino: contaSchema,
    valor: dinheiroSchema,
  })
  .strict();

export const gerenteInputSchema = z
  .object({
    cpf: cpfSchema,
    nome: z.string().min(1),
    email: z.string().email(),
    telefone: z.string().min(1),
    senha: z.string().min(1),
  })
  .strict();

export const messageEnvelopeSchema = z
  .object({
    sagaId: z.string().uuid().optional(),
    tipo: z.string().min(1),
    timestamp: timestampSchema,
    payload: z.record(z.unknown()),
  })
  .strict();

export const replyEnvelopeSchema = z
  .object({
    sagaId: z.string().uuid(),
    tipo: z.string().min(1),
    timestamp: timestampSchema,
    status: z.enum([ReplyStatus.SUCESSO, ReplyStatus.FALHA]),
    erro: z.string().nullable(),
    payload: z.record(z.unknown()),
  })
  .strict();

export const erroBodySchema = z
  .object({
    status: z.number().int(),
    erro: z.string(),
    mensagem: z.string(),
  })
  .strict();

export const authErrorBodySchema = z
  .object({
    auth: z.literal(false),
    message: z.enum([
      AuthMessages.TOKEN_AUSENTE,
      AuthMessages.TOKEN_INVALIDO,
      AuthMessages.LOGIN_INVALIDO,
    ]),
  })
  .strict();

export const sagaTypeSchema = z.enum([
  SagaType.APROVAR_CLIENTE,
  SagaType.INSERIR_GERENTE,
  SagaType.REMOVER_GERENTE,
]);

export const eventTypeSchema = z.enum([
  EventType.CRIADO,
  EventType.SAQUE,
  EventType.DEPOSITO,
  EventType.TRANSFERENCIA_ORIGEM,
  EventType.TRANSFERENCIA_DESTINO,
  EventType.GERENTE_ALTERADO,
]);

export const jobStatusSchema = z.enum([JobStatus.PENDENTE, JobStatus.CONCLUIDO, JobStatus.FALHA]);
export const resultTypeSchema = z.enum([ResultType.RESOURCE, ResultType.INLINE]);
export const perfilSchema = z.enum([Perfil.CLIENTE, Perfil.GERENTE]);
