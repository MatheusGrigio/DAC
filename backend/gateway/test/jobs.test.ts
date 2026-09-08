import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import { buildApp } from '../src/app.ts';
import { loadConfig } from '../src/config.ts';
import { jobKey } from '../src/redis/jobs.ts';
import { CommandTypes } from '../src/types/command-types.ts';
import { JobStatus, ResultType } from '../src/types/enums.ts';
import type { MessageEnvelope } from '../src/types/envelopes.ts';
import type { MsFetch } from '../src/http/ms-client.ts';
import { MemoryStore } from './memory-store.ts';

const CPF = '12912861012';
const GERENTE = '98574307084';
const APROVAR = '22233344405';

function testConfig() {
  return loadConfig({
    JWT_SECRET: 'test-secret-f14',
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

const loginFetch: MsFetch = async (url, init) => {
  if (url.endsWith('/auth/verificar')) {
    const body = JSON.parse(String(init.body ?? '{}')) as { email?: string };
    if (body.email === 'cli1@bantads.com.br') {
      return {
        status: 200,
        headers: jsonHeaders({ 'content-type': 'application/json' }),
        text: async () => JSON.stringify({ cpf: CPF, tipo: 'CLIENTE' }),
      };
    }
    if (body.email === 'ger1@bantads.com.br') {
      return {
        status: 200,
        headers: jsonHeaders({ 'content-type': 'application/json' }),
        text: async () => JSON.stringify({ cpf: GERENTE, tipo: 'GERENTE' }),
      };
    }
    return { status: 401, headers: jsonHeaders(), text: async () => '{}' };
  }
  if (url.endsWith(`/clientes/${CPF}`)) {
    return {
      status: 200,
      headers: jsonHeaders({ 'content-type': 'application/json' }),
      text: async () =>
        JSON.stringify({ cpf: CPF, nome: 'Catharyna', email: 'cli1@bantads.com.br' }),
    };
  }
  if (url.endsWith(`/gerentes/${GERENTE}`)) {
    return {
      status: 200,
      headers: jsonHeaders({ 'content-type': 'application/json' }),
      text: async () =>
        JSON.stringify({ cpf: GERENTE, nome: 'Geniéve', email: 'ger1@bantads.com.br' }),
    };
  }
  return {
    status: 404,
    headers: jsonHeaders({ 'content-type': 'application/json' }),
    text: async () => JSON.stringify({ status: 404, erro: 'Not Found', mensagem: 'não' }),
  };
};

class MemoryPublisher {
  readonly published: MessageEnvelope[] = [];

  async publish(envelope: MessageEnvelope): Promise<void> {
    this.published.push(envelope);
  }
}

async function login(app: Awaited<ReturnType<typeof buildApp>>, email: string): Promise<string> {
  const response = await app.inject({
    method: 'POST',
    url: '/login',
    payload: { email, senha: 'tads' },
  });
  return (response.json() as { token: string }).token;
}

describe('F14 gateway jobs and R9', () => {
  it('T09 GERENTE POST aprovacao returns 202 Location and publishes saga.cmd', async () => {
    const store = new MemoryStore();
    const publisher = new MemoryPublisher();
    const app = await buildApp({
      config: testConfig(),
      store,
      fetchImpl: loginFetch,
      publisher,
    });
    const token = await login(app, 'ger1@bantads.com.br');
    const response = await app.inject({
      method: 'POST',
      url: `/solicitacoes/${APROVAR}/aprovacao`,
      headers: { 'x-access-token': token },
    });
    assert.equal(response.statusCode, 202);
    const body = response.json() as { jobId: string; status: string; _links?: unknown };
    assert.equal(body.status, JobStatus.PENDENTE);
    assert.match(body.jobId, /^[0-9a-f-]{36}$/i);
    assert.equal(response.headers.location, `/jobs/${body.jobId}/status`);
    assert.equal(body._links, undefined);
    assert.equal(publisher.published.length, 1);
    const envelope = publisher.published[0];
    assert.equal(envelope.sagaId, body.jobId);
    assert.equal(envelope.tipo, CommandTypes.APROVAR_CLIENTE);
    assert.equal(envelope.payload.cpf, APROVAR);
    assert.equal(envelope.payload.solicitadoPorCpf, GERENTE);
    const stored = await store.get(jobKey(body.jobId));
    assert.ok(stored);
    await app.close();
  });

  it('T09f does not pre-validate and still returns 202', async () => {
    const publisher = new MemoryPublisher();
    const app = await buildApp({
      config: testConfig(),
      store: new MemoryStore(),
      fetchImpl: loginFetch,
      publisher,
    });
    const token = await login(app, 'ger1@bantads.com.br');
    const missing = await app.inject({
      method: 'POST',
      url: '/solicitacoes/00000000000/aprovacao',
      headers: { 'x-access-token': token },
    });
    assert.equal(missing.statusCode, 202);
    assert.equal(publisher.published[0]?.payload.cpf, '00000000000');
    await app.close();
  });

  it('CLIENTE cannot approve', async () => {
    const app = await buildApp({
      config: testConfig(),
      store: new MemoryStore(),
      fetchImpl: loginFetch,
      publisher: new MemoryPublisher(),
    });
    const token = await login(app, 'cli1@bantads.com.br');
    const response = await app.inject({
      method: 'POST',
      url: `/solicitacoes/${APROVAR}/aprovacao`,
      headers: { 'x-access-token': token },
    });
    assert.equal(response.statusCode, 403);
    await app.close();
  });

  it('GET job status and result follow the contract', async () => {
    const store = new MemoryStore();
    const app = await buildApp({
      config: testConfig(),
      store,
      fetchImpl: loginFetch,
      publisher: new MemoryPublisher(),
    });
    const token = await login(app, 'ger1@bantads.com.br');
    const missing = await app.inject({
      method: 'GET',
      url: '/jobs/8f14e45f-ceea-4e1b-9d2a-52f7254b1f0b/status',
      headers: { 'x-access-token': token },
    });
    assert.equal(missing.statusCode, 404);

    const created = await app.inject({
      method: 'POST',
      url: `/solicitacoes/${APROVAR}/aprovacao`,
      headers: { 'x-access-token': token },
    });
    const jobId = (created.json() as { jobId: string }).jobId;
    const pending = await app.inject({
      method: 'GET',
      url: `/jobs/${jobId}/status`,
      headers: { 'x-access-token': token },
    });
    assert.equal(pending.statusCode, 200);
    assert.equal((pending.json() as { status: string }).status, JobStatus.PENDENTE);
    assert.equal((pending.json() as { _links?: unknown })._links, undefined);

    const pendingResult = await app.inject({
      method: 'GET',
      url: `/jobs/${jobId}/result`,
      headers: { 'x-access-token': token },
    });
    assert.equal(pendingResult.statusCode, 409);

    await store.set(
      jobKey(jobId),
      JSON.stringify({
        jobId,
        status: JobStatus.CONCLUIDO,
        resultType: ResultType.RESOURCE,
        dominio: 'clientes',
        resourceId: APROVAR,
        cpf: GERENTE,
      }),
    );
    const resourceResult = await app.inject({
      method: 'GET',
      url: `/jobs/${jobId}/result`,
      headers: { 'x-access-token': token },
    });
    assert.equal(resourceResult.statusCode, 409);

    const inlineId = 'aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee';
    await store.set(
      jobKey(inlineId),
      JSON.stringify({
        jobId: inlineId,
        status: JobStatus.CONCLUIDO,
        resultType: ResultType.INLINE,
        resultado: { mensagem: 'ok' },
      }),
    );
    const inline = await app.inject({
      method: 'GET',
      url: `/jobs/${inlineId}/result`,
      headers: { 'x-access-token': token },
    });
    assert.equal(inline.statusCode, 200);
    assert.deepEqual(inline.json(), { mensagem: 'ok' });
    assert.equal((inline.json() as { _links?: unknown })._links, undefined);

    const cliente = await login(app, 'cli1@bantads.com.br');
    const owned = await app.inject({
      method: 'GET',
      url: `/jobs/${jobId}/status`,
      headers: { 'x-access-token': cliente },
    });
    assert.equal(owned.statusCode, 403);
    await app.close();
  });

  it('T13 POST /gerentes returns 202 without pre-validating unique email', async () => {
    const publisher = new MemoryPublisher();
    const app = await buildApp({
      config: testConfig(),
      store: new MemoryStore(),
      fetchImpl: loginFetch,
      publisher,
    });
    const token = await login(app, 'ger1@bantads.com.br');
    const body = {
      cpf: '55667788990',
      nome: 'Gumercindo',
      email: 'ger1@bantads.com.br',
      telefone: '41988880005',
      senha: 'tads',
    };
    const response = await app.inject({
      method: 'POST',
      url: '/gerentes',
      headers: { 'x-access-token': token },
      payload: body,
    });
    assert.equal(response.statusCode, 202);
    const job = response.json() as { jobId: string; status: string; _links?: unknown };
    assert.equal(job.status, JobStatus.PENDENTE);
    assert.equal(response.headers.location, `/jobs/${job.jobId}/status`);
    assert.equal(job._links, undefined);
    assert.equal(publisher.published[0]?.tipo, CommandTypes.INSERIR_GERENTE);
    assert.equal(publisher.published[0]?.payload.email, 'ger1@bantads.com.br');
    assert.equal(publisher.published[0]?.payload.senha, 'tads');
    const bad = await app.inject({
      method: 'POST',
      url: '/gerentes',
      headers: { 'x-access-token': token },
      payload: { cpf: '55667788990', nome: 'Gumercindo' },
    });
    assert.equal(bad.statusCode, 400);
    const cliente = await login(app, 'cli1@bantads.com.br');
    const forbidden = await app.inject({
      method: 'POST',
      url: '/gerentes',
      headers: { 'x-access-token': cliente },
      payload: body,
    });
    assert.equal(forbidden.statusCode, 403);
    await app.close();
  });

  it('T15 DELETE self is 403 and other CPF is 202', async () => {
    const publisher = new MemoryPublisher();
    const app = await buildApp({
      config: testConfig(),
      store: new MemoryStore(),
      fetchImpl: loginFetch,
      publisher,
    });
    const token = await login(app, 'ger1@bantads.com.br');
    const self = await app.inject({
      method: 'DELETE',
      url: `/gerentes/${GERENTE}`,
      headers: { 'x-access-token': token },
    });
    assert.equal(self.statusCode, 403);
    assert.equal(publisher.published.length, 0);
    const other = await app.inject({
      method: 'DELETE',
      url: '/gerentes/40501740066',
      headers: { 'x-access-token': token },
    });
    assert.equal(other.statusCode, 202);
    assert.equal(publisher.published[0]?.tipo, CommandTypes.REMOVER_GERENTE);
    assert.equal(publisher.published[0]?.payload.cpf, '40501740066');
    assert.equal((other.json() as { _links?: unknown })._links, undefined);
    await app.close();
  });
});
