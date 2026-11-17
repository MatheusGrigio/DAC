import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import { buildApp } from '../src/app.ts';
import { loadConfig } from '../src/config.ts';
import type { MsFetch } from '../src/http/ms-client.ts';
import { MemoryStore } from './memory-store.ts';

const CPF = '12912861012';

function testConfig() {
  return loadConfig({
    JWT_SECRET: 'test-secret-f11',
    GATEWAY_PUBLIC_URL: 'http://localhost:3000',
    CORS_ORIGIN: 'http://localhost:4200',
    AUTH_URL: 'http://auth:8080',
    CLIENTE_URL: 'http://cliente:8080',
    GERENTE_URL: 'http://gerente:8080',
    CONTA_URL: 'http://conta:8080',
  });
}

function jsonHeaders(init?: HeadersInit): Headers {
  return new Headers(init);
}

function mockFetch(
  handler: (url: string, init: RequestInit) => { status: number; body: unknown },
): MsFetch {
  return async (url, init) => {
    const result = handler(url, init);
    return {
      status: result.status,
      headers: jsonHeaders({ 'content-type': 'application/json' }),
      text: async () => JSON.stringify(result.body),
    };
  };
}

const rebootFetch = mockFetch((url) => {
  if (url.endsWith('/internal/reboot')) {
    if (url.includes('auth:8080')) {
      return { status: 200, body: { status: 'ok', usuarios: 9 } };
    }
    if (url.includes('cliente:8080')) {
      return { status: 200, body: { status: 'ok', clientes: 5 } };
    }
    if (url.includes('gerente:8080')) {
      return { status: 200, body: { status: 'ok', gerentes: 4 } };
    }
    if (url.includes('conta:8080')) {
      return { status: 200, body: { status: 'ok', contas: 5, eventos: 22 } };
    }
  }
  if (url.endsWith('/auth/verificar')) {
    return { status: 200, body: { cpf: CPF, tipo: 'CLIENTE' } };
  }
  if (url.endsWith(`/clientes/${CPF}`)) {
    return { status: 200, body: { cpf: CPF, nome: 'Catharyna', email: 'cli1@bantads.com.br' } };
  }
  return { status: 404, body: { status: 404, erro: 'Not Found', mensagem: 'não encontrado' } };
});

describe('F11 gateway reboot', () => {
  it('T00 POST /reboot returns seed counts without links', async () => {
    const app = await buildApp({
      config: testConfig(),
      store: new MemoryStore(),
      fetchImpl: rebootFetch,
    });
    const first = await app.inject({ method: 'POST', url: '/reboot' });
    assert.equal(first.statusCode, 200);
    assert.deepEqual(first.json(), { status: 'ok', clientes: 5, gerentes: 4, contas: 5 });
    assert.equal((first.json() as { _links?: unknown })._links, undefined);
    const second = await app.inject({ method: 'POST', url: '/reboot' });
    assert.equal(second.statusCode, 200);
    assert.deepEqual(second.json(), first.json());
    await app.close();
  });

  it('T00b GET /health is UP without links', async () => {
    const app = await buildApp({
      config: testConfig(),
      store: new MemoryStore(),
      fetchImpl: rebootFetch,
    });
    const response = await app.inject({ method: 'GET', url: '/health' });
    assert.equal(response.statusCode, 200);
    assert.deepEqual(response.json(), { status: 'UP' });
    assert.equal((response.json() as { _links?: unknown })._links, undefined);
    await app.close();
  });

  it('reboot flushes Redis sessions', async () => {
    const store = new MemoryStore();
    const app = await buildApp({ config: testConfig(), store, fetchImpl: rebootFetch });
    const login = await app.inject({
      method: 'POST',
      url: '/login',
      payload: { email: 'cli1@bantads.com.br', senha: 'tads' },
    });
    const token = (login.json() as { token: string }).token;
    const before = await app.inject({
      method: 'GET',
      url: `/clientes/${CPF}`,
      headers: { 'x-access-token': token },
    });
    assert.equal(before.statusCode, 200);
    await app.inject({ method: 'POST', url: '/reboot' });
    const after = await app.inject({
      method: 'GET',
      url: `/clientes/${CPF}`,
      headers: { 'x-access-token': token },
    });
    assert.equal(after.statusCode, 401);
    assert.deepEqual(after.json(), { auth: false, message: 'Falha ao autenticar o token.' });
    await app.close();
  });
});
