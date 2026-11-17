import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import { buildApp } from '../src/app.ts';
import { loadConfig } from '../src/config.ts';
import { jobKey } from '../src/redis/jobs.ts';
import { JobStatus, ResultType } from '../src/types/enums.ts';
import type { MsFetch } from '../src/http/ms-client.ts';
import { MemoryStore } from './memory-store.ts';

const CPF = '12912861012';
const GERENTE = '98574307084';

function testConfig() {
  return loadConfig({
    JWT_SECRET: 'test-secret-f17',
    GATEWAY_PUBLIC_URL: 'http://localhost:3000',
    CORS_ORIGIN: 'http://localhost:4200',
    AUTH_URL: 'http://auth:8080',
    CLIENTE_URL: 'http://cliente:8080',
    GERENTE_URL: 'http://gerente:8080',
    CONTA_URL: 'http://conta:8080',
  });
}

function jsonHeaders(): Headers {
  return new Headers({ 'content-type': 'application/json' });
}

const relatorioFetch: MsFetch = async (url, init) => {
  if (url.endsWith('/auth/verificar')) {
    const body = JSON.parse(String(init.body ?? '{}')) as { email?: string };
    if (body.email === 'cli1@bantads.com.br') {
      return {
        status: 200,
        headers: jsonHeaders(),
        text: async () => JSON.stringify({ cpf: CPF, tipo: 'CLIENTE' }),
      };
    }
    if (body.email === 'ger1@bantads.com.br') {
      return {
        status: 200,
        headers: jsonHeaders(),
        text: async () => JSON.stringify({ cpf: GERENTE, tipo: 'GERENTE' }),
      };
    }
    return { status: 401, headers: jsonHeaders(), text: async () => '{}' };
  }
  if (url.endsWith(`/clientes/${CPF}`)) {
    return {
      status: 200,
      headers: jsonHeaders(),
      text: async () =>
        JSON.stringify({ cpf: CPF, nome: 'Catharyna', email: 'cli1@bantads.com.br' }),
    };
  }
  if (url.endsWith(`/gerentes/${GERENTE}`)) {
    return {
      status: 200,
      headers: jsonHeaders(),
      text: async () =>
        JSON.stringify({ cpf: GERENTE, nome: 'Geniéve', email: 'ger1@bantads.com.br' }),
    };
  }
  if (url.endsWith('/clientes')) {
    return {
      status: 200,
      headers: jsonHeaders(),
      text: async () =>
        JSON.stringify({
          clientes: [
            {
              cpf: '58872160006',
              nome: 'Cutardo',
              email: 'cli4@bantads.com.br',
              salario: '500.00',
            },
            {
              cpf: CPF,
              nome: 'Catharyna',
              email: 'cli1@bantads.com.br',
              salario: '10000.00',
            },
            {
              cpf: '85733854057',
              nome: 'Catianna',
              email: 'cli3@bantads.com.br',
              salario: '3000.00',
            },
            {
              cpf: '09506382000',
              nome: 'Cleuddônio',
              email: 'cli2@bantads.com.br',
              salario: '20000.00',
            },
            {
              cpf: '76179646090',
              nome: 'Coândrya',
              email: 'cli5@bantads.com.br',
              salario: '1500.00',
            },
          ],
        }),
    };
  }
  if (url.endsWith('/internal/saldos')) {
    return {
      status: 200,
      headers: jsonHeaders(),
      text: async () =>
        JSON.stringify({
          '12912861012': { saldo: '800.00', numero: '1291', cpfGerente: GERENTE },
          '09506382000': { saldo: '10000.00', numero: '0950', cpfGerente: '64065268052' },
          '85733854057': { saldo: '200.00', numero: '8573', cpfGerente: '23862179060' },
          '58872160006': { saldo: '150000.00', numero: '5887', cpfGerente: GERENTE },
          '76179646090': { saldo: '1500.00', numero: '7617', cpfGerente: '64065268052' },
        }),
    };
  }
  if (url.endsWith('/gerentes')) {
    return {
      status: 200,
      headers: jsonHeaders(),
      text: async () =>
        JSON.stringify({
          gerentes: [
            { cpf: GERENTE, nome: 'Geniéve' },
            { cpf: '64065268052', nome: 'Godophredo' },
            { cpf: '23862179060', nome: 'Gyândula' },
            { cpf: '40501740066', nome: 'Gadamântio' },
          ],
        }),
    };
  }
  return {
    status: 404,
    headers: jsonHeaders(),
    text: async () => JSON.stringify({ status: 404, erro: 'Not Found', mensagem: 'não' }),
  };
};

async function login(app: Awaited<ReturnType<typeof buildApp>>, email: string): Promise<string> {
  const response = await app.inject({
    method: 'POST',
    url: '/login',
    payload: { email, senha: 'tads' },
  });
  return (response.json() as { token: string }).token;
}

async function waitJob(
  app: Awaited<ReturnType<typeof buildApp>>,
  token: string,
  jobId: string,
): Promise<{ status: string; resultType?: string }> {
  for (let i = 0; i < 30; i += 1) {
    const response = await app.inject({
      method: 'GET',
      url: `/jobs/${jobId}/status`,
      headers: { 'x-access-token': token },
    });
    const body = response.json() as { status: string; resultType?: string };
    if (body.status === JobStatus.CONCLUIDO || body.status === JobStatus.FALHA) {
      return body;
    }
    await new Promise((resolve) => setTimeout(resolve, 10));
  }
  throw new Error(`job ${jobId} não terminou`);
}

describe('F17 relatório R16', () => {
  it('T16 GERENTE GET /relatorios/clientes returns 202 then inline list', async () => {
    const store = new MemoryStore();
    const app = await buildApp({
      config: testConfig(),
      store,
      fetchImpl: relatorioFetch,
    });
    const token = await login(app, 'ger1@bantads.com.br');
    const accepted = await app.inject({
      method: 'GET',
      url: '/relatorios/clientes',
      headers: { 'x-access-token': token },
    });
    assert.equal(accepted.statusCode, 202);
    const job = accepted.json() as {
      jobId: string;
      status: string;
      cpf?: string;
      _links?: unknown;
    };
    assert.equal(job.status, JobStatus.PENDENTE);
    assert.equal(job.cpf, undefined);
    assert.equal(job._links, undefined);
    assert.equal(accepted.headers.location, `/jobs/${job.jobId}/status`);
    const stored = JSON.parse((await store.get(jobKey(job.jobId))) ?? '{}') as { cpf?: string };
    assert.equal(stored.cpf, GERENTE);

    const done = await waitJob(app, token, job.jobId);
    assert.equal(done.status, JobStatus.CONCLUIDO);
    assert.equal(done.resultType, ResultType.INLINE);

    const result = await app.inject({
      method: 'GET',
      url: `/jobs/${job.jobId}/result`,
      headers: { 'x-access-token': token },
    });
    assert.equal(result.statusCode, 200);
    const body = result.json() as {
      clientes: Array<{ nome: string; numeroConta: string; nomeGerente: string }>;
      _links?: unknown;
    };
    assert.equal(body._links, undefined);
    assert.deepEqual(
      body.clientes.map((item) => item.nome),
      ['Catharyna', 'Catianna', 'Cleuddônio', 'Coândrya', 'Cutardo'],
    );
    assert.equal(body.clientes[0]?.numeroConta, '1291');
    assert.equal(body.clientes[0]?.nomeGerente, 'Geniéve');
    await app.close();
  });

  it('CLIENTE cannot request the report', async () => {
    const app = await buildApp({
      config: testConfig(),
      store: new MemoryStore(),
      fetchImpl: relatorioFetch,
    });
    const token = await login(app, 'cli1@bantads.com.br');
    const response = await app.inject({
      method: 'GET',
      url: '/relatorios/clientes',
      headers: { 'x-access-token': token },
    });
    assert.equal(response.statusCode, 403);
    await app.close();
  });
});
