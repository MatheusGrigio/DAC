import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import jwt from 'jsonwebtoken';
import { buildApp } from '../src/app.ts';
import { loadConfig } from '../src/config.ts';
import { rewriteHref, rewriteLinks } from '../src/http/hateoas.ts';
import type { MsFetch } from '../src/http/ms-client.ts';
import { MemoryStore } from './memory-store.ts';

const CPF = '12912861012';
const GERENTE_CPF = '98574307084';

function testConfig() {
  return loadConfig({
    JWT_SECRET: 'test-secret-f10',
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

const seedFetch = mockFetch((url) => {
  if (url.endsWith('/auth/verificar')) {
    return { status: 401, body: {} };
  }
  return { status: 404, body: { status: 404, erro: 'Not Found', mensagem: 'não encontrado' } };
});

describe('F10 gateway auth', () => {
  it('T02a GET /clientes/{cpf} without token', async () => {
    const app = await buildApp({
      config: testConfig(),
      store: new MemoryStore(),
      fetchImpl: seedFetch,
    });
    const response = await app.inject({ method: 'GET', url: `/clientes/${CPF}` });
    assert.equal(response.statusCode, 401);
    assert.deepEqual(response.json(), { auth: false, message: 'Token não fornecido.' });
    assert.equal(response.json()._links, undefined);
    await app.close();
  });

  it('T02b invalid token', async () => {
    const app = await buildApp({
      config: testConfig(),
      store: new MemoryStore(),
      fetchImpl: seedFetch,
    });
    const response = await app.inject({
      method: 'GET',
      url: `/clientes/${CPF}`,
      headers: { 'x-access-token': 'nao.e.jwt' },
    });
    assert.equal(response.statusCode, 401);
    assert.deepEqual(response.json(), { auth: false, message: 'Falha ao autenticar o token.' });
    await app.close();
  });

  it('T02c login with wrong password', async () => {
    const app = await buildApp({
      config: testConfig(),
      store: new MemoryStore(),
      fetchImpl: seedFetch,
    });
    const response = await app.inject({
      method: 'POST',
      url: '/login',
      payload: { email: 'cli1@bantads.com.br', senha: 'errada' },
    });
    assert.equal(response.statusCode, 401);
    assert.deepEqual(response.json(), { auth: false, message: 'Login inválido!' });
    await app.close();
  });

  it('T02d login seed cliente', async () => {
    const fetchImpl = mockFetch((url) => {
      if (url.endsWith('/auth/verificar')) {
        return { status: 200, body: { cpf: CPF, tipo: 'CLIENTE' } };
      }
      if (url.endsWith(`/clientes/${CPF}`)) {
        return {
          status: 200,
          body: {
            cpf: CPF,
            nome: 'Catharyna',
            email: 'cli1@bantads.com.br',
            _links: { self: { href: 'http://cliente:8080/clientes/12912861012' } },
          },
        };
      }
      return { status: 404, body: {} };
    });
    const app = await buildApp({ config: testConfig(), store: new MemoryStore(), fetchImpl });
    const response = await app.inject({
      method: 'POST',
      url: '/login',
      payload: { email: 'cli1@bantads.com.br', senha: 'tads' },
    });
    assert.equal(response.statusCode, 200);
    const body = response.json() as {
      auth: boolean;
      token: string;
      tipo: string;
      usuario: { cpf: string; nome: string; email: string };
      _links?: unknown;
    };
    assert.equal(body.auth, true);
    assert.equal(body.tipo, 'CLIENTE');
    assert.equal(body.usuario.cpf, CPF);
    assert.equal(body.usuario.nome, 'Catharyna');
    assert.equal(body.usuario.email, 'cli1@bantads.com.br');
    assert.ok(body.token.length > 20);
    assert.equal(body._links, undefined);
    await app.close();
  });

  it('T02e login seed gerente', async () => {
    const fetchImpl = mockFetch((url) => {
      if (url.endsWith('/auth/verificar')) {
        return { status: 200, body: { cpf: GERENTE_CPF, tipo: 'GERENTE' } };
      }
      if (url.endsWith(`/gerentes/${GERENTE_CPF}`)) {
        return {
          status: 200,
          body: { cpf: GERENTE_CPF, nome: 'Geniéve', email: 'ger1@bantads.com.br' },
        };
      }
      return { status: 404, body: {} };
    });
    const app = await buildApp({ config: testConfig(), store: new MemoryStore(), fetchImpl });
    const response = await app.inject({
      method: 'POST',
      url: '/login',
      payload: { email: 'ger1@bantads.com.br', senha: 'tads' },
    });
    assert.equal(response.statusCode, 200);
    const body = response.json() as { tipo: string; usuario: { cpf: string } };
    assert.equal(body.tipo, 'GERENTE');
    assert.equal(body.usuario.cpf, GERENTE_CPF);
    await app.close();
  });

  it('T02f logout then reuse token', async () => {
    const fetchImpl = mockFetch((url) => {
      if (url.endsWith('/auth/verificar')) {
        return { status: 200, body: { cpf: CPF, tipo: 'CLIENTE' } };
      }
      if (url.endsWith(`/clientes/${CPF}`)) {
        return {
          status: 200,
          body: { cpf: CPF, nome: 'Catharyna', email: 'cli1@bantads.com.br' },
        };
      }
      return { status: 404, body: {} };
    });
    const app = await buildApp({ config: testConfig(), store: new MemoryStore(), fetchImpl });
    const login = await app.inject({
      method: 'POST',
      url: '/login',
      payload: { email: 'cli1@bantads.com.br', senha: 'tads' },
    });
    const token = (login.json() as { token: string }).token;
    const logout = await app.inject({
      method: 'POST',
      url: '/logout',
      headers: { 'x-access-token': token },
    });
    assert.equal(logout.statusCode, 204);
    const reuse = await app.inject({
      method: 'GET',
      url: `/clientes/${CPF}`,
      headers: { 'x-access-token': token },
    });
    assert.equal(reuse.statusCode, 401);
    assert.deepEqual(reuse.json(), { auth: false, message: 'Falha ao autenticar o token.' });
    await app.close();
  });

  it('health is UP without links', async () => {
    const app = await buildApp({
      config: testConfig(),
      store: new MemoryStore(),
      fetchImpl: seedFetch,
    });
    const response = await app.inject({ method: 'GET', url: '/health' });
    assert.equal(response.statusCode, 200);
    assert.deepEqual(response.json(), { status: 'UP' });
    await app.close();
  });

  it('CORS allows x-access-token from Angular origin', async () => {
    const app = await buildApp({
      config: testConfig(),
      store: new MemoryStore(),
      fetchImpl: seedFetch,
    });
    const response = await app.inject({
      method: 'OPTIONS',
      url: '/login',
      headers: {
        origin: 'http://localhost:4200',
        'access-control-request-method': 'POST',
        'access-control-request-headers': 'x-access-token',
      },
    });
    assert.equal(response.statusCode, 204);
    assert.equal(response.headers['access-control-allow-origin'], 'http://localhost:4200');
    const allowHeaders = String(
      response.headers['access-control-allow-headers'] ?? '',
    ).toLowerCase();
    assert.ok(allowHeaders.includes('x-access-token'));
    await app.close();
  });

  it('rewrites internal HATEOAS href to the gateway public URL', () => {
    const rewritten = rewriteLinks(
      { _links: { self: { href: 'http://cliente:8080/clientes/12912861012' } } },
      'http://localhost:3000',
    );
    assert.deepEqual(rewritten, {
      _links: { self: { href: 'http://localhost:3000/clientes/12912861012' } },
    });
    assert.equal(
      rewriteHref('http://gerente:8080/gerentes/98574307084', 'http://localhost:3000'),
      'http://localhost:3000/gerentes/98574307084',
    );
  });

  it('expired JWT is rejected', async () => {
    const app = await buildApp({
      config: testConfig(),
      store: new MemoryStore(),
      fetchImpl: seedFetch,
    });
    const token = jwt.sign(
      {
        cpf: CPF,
        tipo: 'CLIENTE',
        jti: 'expired-jti',
        exp: Math.floor(Date.now() / 1000) - 60,
      },
      'test-secret-f10',
    );
    const response = await app.inject({
      method: 'GET',
      url: `/clientes/${CPF}`,
      headers: { 'x-access-token': token },
    });
    assert.equal(response.statusCode, 401);
    assert.deepEqual(response.json(), { auth: false, message: 'Falha ao autenticar o token.' });
    await app.close();
  });

  it('missing Redis session is treated as inactivity', async () => {
    const fetchImpl = mockFetch((url) => {
      if (url.endsWith('/auth/verificar')) {
        return { status: 200, body: { cpf: CPF, tipo: 'CLIENTE' } };
      }
      if (url.endsWith(`/clientes/${CPF}`)) {
        return { status: 200, body: { cpf: CPF, nome: 'Catharyna', email: 'cli1@bantads.com.br' } };
      }
      return { status: 404, body: {} };
    });
    const store = new MemoryStore();
    const app = await buildApp({ config: testConfig(), store, fetchImpl });
    const login = await app.inject({
      method: 'POST',
      url: '/login',
      payload: { email: 'cli1@bantads.com.br', senha: 'tads' },
    });
    const token = (login.json() as { token: string }).token;
    const jti = (jwt.decode(token) as { jti: string }).jti;
    await store.del(`sessao:${jti}`);
    const response = await app.inject({
      method: 'GET',
      url: `/clientes/${CPF}`,
      headers: { 'x-access-token': token },
    });
    assert.equal(response.statusCode, 401);
    assert.deepEqual(response.json(), { auth: false, message: 'Falha ao autenticar o token.' });
    await app.close();
  });

  it('revoked jti is rejected even if the session key remains', async () => {
    const fetchImpl = mockFetch((url) => {
      if (url.endsWith('/auth/verificar')) {
        return { status: 200, body: { cpf: CPF, tipo: 'CLIENTE' } };
      }
      if (url.endsWith(`/clientes/${CPF}`)) {
        return { status: 200, body: { cpf: CPF, nome: 'Catharyna', email: 'cli1@bantads.com.br' } };
      }
      return { status: 404, body: {} };
    });
    const store = new MemoryStore();
    const app = await buildApp({ config: testConfig(), store, fetchImpl });
    const login = await app.inject({
      method: 'POST',
      url: '/login',
      payload: { email: 'cli1@bantads.com.br', senha: 'tads' },
    });
    const token = (login.json() as { token: string }).token;
    const jti = (jwt.decode(token) as { jti: string }).jti;
    await store.set(`revogado:${jti}`, '1');
    const response = await app.inject({
      method: 'GET',
      url: `/clientes/${CPF}`,
      headers: { 'x-access-token': token },
    });
    assert.equal(response.statusCode, 401);
    assert.deepEqual(response.json(), { auth: false, message: 'Falha ao autenticar o token.' });
    await app.close();
  });

  it('CLIENTE cannot read another cliente', async () => {
    const fetchImpl = mockFetch((url) => {
      if (url.endsWith('/auth/verificar')) {
        return { status: 200, body: { cpf: CPF, tipo: 'CLIENTE' } };
      }
      if (url.endsWith(`/clientes/${CPF}`)) {
        return { status: 200, body: { cpf: CPF, nome: 'Catharyna', email: 'cli1@bantads.com.br' } };
      }
      return { status: 200, body: { cpf: '00000000000', nome: 'Outro', email: 'x@y.com' } };
    });
    const app = await buildApp({ config: testConfig(), store: new MemoryStore(), fetchImpl });
    const login = await app.inject({
      method: 'POST',
      url: '/login',
      payload: { email: 'cli1@bantads.com.br', senha: 'tads' },
    });
    const token = (login.json() as { token: string }).token;
    const other = await app.inject({
      method: 'GET',
      url: '/clientes/76179646090',
      headers: { 'x-access-token': token },
    });
    assert.equal(other.statusCode, 403);
    assert.equal((other.json() as { status: number }).status, 403);
    await app.close();
  });
});
