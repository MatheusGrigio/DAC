import { Redis } from 'ioredis';
import type { KeyValueStore } from './store.js';

export class RedisStore implements KeyValueStore {
  constructor(private readonly redis: Redis) {}

  get(key: string): Promise<string | null> {
    return this.redis.get(key);
  }

  async set(key: string, value: string, ttlSeconds?: number): Promise<void> {
    if (ttlSeconds === undefined) {
      await this.redis.set(key, value);
      return;
    }
    await this.redis.set(key, value, 'EX', ttlSeconds);
  }

  del(...keys: string[]): Promise<number> {
    if (keys.length === 0) {
      return Promise.resolve(0);
    }
    return this.redis.del(...keys);
  }

  async expire(key: string, ttlSeconds: number): Promise<void> {
    await this.redis.expire(key, ttlSeconds);
  }

  ttl(key: string): Promise<number> {
    return this.redis.ttl(key);
  }

  async flushdb(): Promise<void> {
    await this.redis.flushdb();
  }
}

export function connectRedis(url: string): Redis {
  return new Redis(url, {
    maxRetriesPerRequest: 2,
    enableReadyCheck: true,
  });
}
