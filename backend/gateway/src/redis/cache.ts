import { CACHE_TTL_SECONDS } from '../config.js';
import type { KeyValueStore } from './store.js';

export function clienteCacheKey(cpf: string): string {
  return `cache:cliente:${cpf}`;
}

export function gerenteCacheKey(cpf: string): string {
  return `cache:gerente:${cpf}`;
}

export async function readCache(store: KeyValueStore, key: string): Promise<unknown | null> {
  const raw = await store.get(key);
  if (!raw) {
    return null;
  }
  try {
    return JSON.parse(raw) as unknown;
  } catch {
    await store.del(key);
    return null;
  }
}

export async function writeCache(store: KeyValueStore, key: string, value: unknown): Promise<void> {
  await store.set(key, JSON.stringify(value), CACHE_TTL_SECONDS);
}
