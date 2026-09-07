import type { FastifyInstance } from 'fastify';
import { randomUUID } from 'node:crypto';
import type { AppConfig } from '../config.js';
import { signAccessToken, tokenExpiry } from '../auth/jwt.js';
import { Erros, authError } from '../http/errors.js';
import { msRequest, type MsFetch } from '../http/ms-client.js';
import { createSession } from '../redis/session.js';
import type { KeyValueStore } from '../redis/store.js';
import { AuthMessages, Perfil } from '../types/enums.js';
import { loginInputSchema } from '../types/schemas.js';
import type { GatewayUser } from '../types/fastify.js';

function textField(source: unknown, key: string): string | undefined {
  if (!source || typeof source !== 'object') {
    return undefined;
  }
  const value = (source as Record<string, unknown>)[key];
  return typeof value === 'string' && value.length > 0 ? value : undefined;
}

async function loadUsuario(
  config: AppConfig,
  fetchImpl: MsFetch | undefined,
  cpf: string,
  tipo: GatewayUser['tipo'],
): Promise<{ cpf: string; nome: string; email: string } | null> {
  const path = tipo === Perfil.CLIENTE ? `/clientes/${cpf}` : `/gerentes/${cpf}`;
  const baseUrl = tipo === Perfil.CLIENTE ? config.clienteUrl : config.gerenteUrl;
  const response = await msRequest({
    baseUrl,
    method: 'GET',
    path,
    headers: { 'X-User-CPF': cpf, 'X-User-Tipo': tipo },
    fetchImpl,
  });
  if (response.status !== 200) {
    return null;
  }
  const nome = textField(response.body, 'nome');
  const email = textField(response.body, 'email');
  const bodyCpf = textField(response.body, 'cpf') ?? cpf;
  if (!nome || !email) {
    return null;
  }
  return { cpf: bodyCpf, nome, email };
}

export function registerLogin(
  app: FastifyInstance,
  deps: { config: AppConfig; store: KeyValueStore; fetchImpl?: MsFetch },
): void {
  app.post('/login', async (request, reply) => {
    const parsed = loginInputSchema.safeParse(request.body);
    if (!parsed.success) {
      return reply.code(400).send(Erros.badRequest('Requisição malformada'));
    }
    const auth = await msRequest({
      baseUrl: deps.config.authUrl,
      method: 'POST',
      path: '/auth/verificar',
      body: parsed.data,
      fetchImpl: deps.fetchImpl,
    });
    if (auth.status !== 200 || !auth.body || typeof auth.body !== 'object') {
      return reply.code(401).send(authError(AuthMessages.LOGIN_INVALIDO));
    }
    const cpf = textField(auth.body, 'cpf');
    const tipoRaw = textField(auth.body, 'tipo');
    if (!cpf || (tipoRaw !== Perfil.CLIENTE && tipoRaw !== Perfil.GERENTE)) {
      return reply.code(401).send(authError(AuthMessages.LOGIN_INVALIDO));
    }
    const usuario = await loadUsuario(deps.config, deps.fetchImpl, cpf, tipoRaw);
    if (!usuario) {
      return reply.code(401).send(authError(AuthMessages.LOGIN_INVALIDO));
    }
    const jti = randomUUID();
    const token = signAccessToken(deps.config.jwtSecret, { cpf, tipo: tipoRaw, jti });
    const exp = tokenExpiry(token);
    await createSession(deps.store, { cpf, tipo: tipoRaw, jti, exp });
    return reply.code(200).send({
      auth: true,
      token,
      tipo: tipoRaw,
      usuario,
    });
  });
}
