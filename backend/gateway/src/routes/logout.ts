import type { FastifyInstance } from 'fastify';
import { revokeSession } from '../redis/session.js';
import type { KeyValueStore } from '../redis/store.js';

export function registerLogout(app: FastifyInstance, store: KeyValueStore): void {
  app.post('/logout', async (request, reply) => {
    const user = request.user;
    if (!user) {
      return;
    }
    await revokeSession(store, user, Math.floor(Date.now() / 1000));
    return reply.code(204).send();
  });
}
