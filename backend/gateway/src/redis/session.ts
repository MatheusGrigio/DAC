import { SESSION_TTL_SECONDS } from '../config.js';
import type { GatewayUser } from '../types/fastify.js';
import type { KeyValueStore } from './store.js';

export type SessionRecord = {
  cpf: string;
  tipo: GatewayUser['tipo'];
  expJwt: number;
};

export function sessionKey(jti: string): string {
  return `sessao:${jti}`;
}

export function sessionByCpfKey(cpf: string): string {
  return `sessao:cpf:${cpf}`;
}

export function revokedKey(jti: string): string {
  return `revogado:${jti}`;
}

export async function createSession(
  store: KeyValueStore,
  user: Pick<GatewayUser, 'cpf' | 'tipo' | 'jti' | 'exp'>,
): Promise<void> {
  const previous = await store.get(sessionByCpfKey(user.cpf));
  if (previous && previous !== user.jti) {
    await store.del(sessionKey(previous));
  }
  const record: SessionRecord = { cpf: user.cpf, tipo: user.tipo, expJwt: user.exp };
  await store.set(sessionKey(user.jti), JSON.stringify(record), SESSION_TTL_SECONDS);
  await store.set(sessionByCpfKey(user.cpf), user.jti, SESSION_TTL_SECONDS);
}

export async function touchSession(store: KeyValueStore, cpf: string, jti: string): Promise<void> {
  await store.expire(sessionKey(jti), SESSION_TTL_SECONDS);
  await store.expire(sessionByCpfKey(cpf), SESSION_TTL_SECONDS);
}

export async function revokeSession(
  store: KeyValueStore,
  user: Pick<GatewayUser, 'cpf' | 'jti' | 'exp'>,
  nowSeconds: number,
): Promise<void> {
  await store.del(sessionKey(user.jti), sessionByCpfKey(user.cpf));
  const remaining = Math.max(1, user.exp - nowSeconds);
  await store.set(revokedKey(user.jti), '1', remaining);
}

export async function isRevoked(store: KeyValueStore, jti: string): Promise<boolean> {
  return (await store.get(revokedKey(jti))) !== null;
}

export async function readSession(
  store: KeyValueStore,
  jti: string,
): Promise<SessionRecord | null> {
  const raw = await store.get(sessionKey(jti));
  if (!raw) {
    return null;
  }
  return JSON.parse(raw) as SessionRecord;
}
