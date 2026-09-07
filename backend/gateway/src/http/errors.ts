import { AuthMessages } from '../types/enums.js';
import type { AuthErrorBody, ErroBody } from '../types/envelopes.js';

export function authError(
  message: (typeof AuthMessages)[keyof typeof AuthMessages],
): AuthErrorBody {
  return { auth: false, message };
}

export function erro(status: number, nome: string, mensagem: string): ErroBody {
  return { status, erro: nome, mensagem };
}

export const Erros = {
  badRequest: (mensagem: string) => erro(400, 'Bad Request', mensagem),
  forbidden: (mensagem = 'Acesso negado') => erro(403, 'Forbidden', mensagem),
  notFound: (mensagem = 'Recurso não encontrado') => erro(404, 'Not Found', mensagem),
  unprocessable: (mensagem: string) => erro(422, 'Unprocessable Entity', mensagem),
  conflict: (mensagem: string) => erro(409, 'Conflict', mensagem),
};
