import type { ReplyStatusName } from './enums.js';

export type JsonObject = Record<string, unknown>;

export interface MessageEnvelope {
  sagaId?: string;
  tipo: string;
  timestamp: string;
  payload: JsonObject;
}

export interface ReplyEnvelope {
  sagaId: string;
  tipo: string;
  timestamp: string;
  status: ReplyStatusName;
  erro: string | null;
  payload: JsonObject;
}

export interface ErroBody {
  status: number;
  erro: string;
  mensagem: string;
}

export interface AuthErrorBody {
  auth: false;
  message: string;
}

export interface LoginInput {
  email: string;
  senha: string;
}
