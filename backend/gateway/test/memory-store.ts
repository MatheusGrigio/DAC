import type { KeyValueStore } from '../../src/redis/store.ts';

type Entry = { value: string; expiresAt: number | null };

export class MemoryStore implements KeyValueStore {
  private readonly data = new Map<string, Entry>();

  async get(key: string): Promise<string | null> {
    const entry = this.data.get(key);
    if (!entry) {
      return null;
    }
    if (entry.expiresAt !== null && entry.expiresAt <= Date.now()) {
      this.data.delete(key);
      return null;
    }
    return entry.value;
  }

  async set(key: string, value: string, ttlSeconds?: number): Promise<void> {
    const expiresAt = ttlSeconds === undefined ? null : Date.now() + ttlSeconds * 1000;
    this.data.set(key, { value, expiresAt });
  }

  async del(...keys: string[]): Promise<number> {
    let removed = 0;
    for (const key of keys) {
      if (this.data.delete(key)) {
        removed += 1;
      }
    }
    return removed;
  }

  async expire(key: string, ttlSeconds: number): Promise<void> {
    const entry = this.data.get(key);
    if (!entry) {
      return;
    }
    entry.expiresAt = Date.now() + ttlSeconds * 1000;
  }

  async ttl(key: string): Promise<number> {
    const entry = this.data.get(key);
    if (!entry) {
      return -2;
    }
    if (entry.expiresAt === null) {
      return -1;
    }
    return Math.max(0, Math.ceil((entry.expiresAt - Date.now()) / 1000));
  }

  async flushdb(): Promise<void> {
    this.data.clear();
  }
}
