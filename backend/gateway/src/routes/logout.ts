import type { FastifyInstance } from 'fastify';
import { authError } from '../http/errors.js';
import { revokeSession } from '../redis/session.js';
import type { KeyValueStore } from '../redis/store.js';
import { AuthMessages } from '../types/enums.js';

export function registerLogout(app: FastifyInstance, store: KeyValueStore): void {
  app.post('/logout', async (request, reply) => {
    const user = request.user;
    if (!user) {
      return reply.code(401).send(authError(AuthMessages.TOKEN_INVALIDO));
    }
    await revokeSession(store, user, Math.floor(Date.now() / 1000));
    return reply.code(204).send();
  });
}
