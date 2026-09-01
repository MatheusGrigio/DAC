import type { FastifyInstance, FastifyReply, FastifyRequest } from 'fastify';
import { accessFor, isAllowed, isPublicRoute, pathnameOf } from './acl.js';
import { verifyAccessToken } from './jwt.js';
import { Erros, authError } from '../http/errors.js';
import { isRevoked, readSession, touchSession } from '../redis/session.js';
import type { KeyValueStore } from '../redis/store.js';
import { AuthMessages } from '../types/enums.js';

export function registerAuthHook(
  app: FastifyInstance,
  deps: { store: KeyValueStore; jwtSecret: string },
): void {
  app.addHook('onRequest', async (request: FastifyRequest, reply: FastifyReply) => {
    if (request.method === 'OPTIONS') {
      return;
    }
    const path = pathnameOf(request.url);
    if (isPublicRoute(request.method, path)) {
      return;
    }

    const token = request.headers['x-access-token'];
    const raw = Array.isArray(token) ? token[0] : token;
    if (!raw) {
      return reply.code(401).send(authError(AuthMessages.TOKEN_AUSENTE));
    }

    let user;
    try {
      user = verifyAccessToken(deps.jwtSecret, raw);
    } catch {
      return reply.code(401).send(authError(AuthMessages.TOKEN_INVALIDO));
    }

    if (await isRevoked(deps.store, user.jti)) {
      return reply.code(401).send(authError(AuthMessages.TOKEN_INVALIDO));
    }
    const session = await readSession(deps.store, user.jti);
    if (!session) {
      return reply.code(401).send(authError(AuthMessages.TOKEN_INVALIDO));
    }

    await touchSession(deps.store, user.cpf, user.jti);
    request.user = user;

    const access = accessFor(request.method, path);
    if (access.kind === 'unknown') {
      return reply.code(404).send(Erros.notFound());
    }
    if (!isAllowed(access, user, path)) {
      return reply.code(403).send(Erros.forbidden());
    }
  });
}
