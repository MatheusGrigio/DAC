import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import { buildApp } from '../src/app.ts';
import { loadConfig } from '../src/config.ts';
import type { MsFetch } from '../src/http/ms-client.ts';
import { MemoryStore } from './memory-store.ts';

const CPF = '12912861012';
const OTHER = '09506382000';
const GERENTE = '98574307084';

function testConfig() {
  return loadConfig({
    JWT_SECRET: 'test-secret-f12',
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
  handler: (url: string, init: RequestInit) => { status: number; body: unknown; location?: string },
): MsFetch {
  return async (url, init) => {
    const result = handler(url, init);
    const headers = jsonHeaders({ 'content-type': 'application/json' });
    if (result.location) {
      headers.set('location', result.location);
    }
    return {
      status: result.status,
      headers,
      text: async () => JSON.stringify(result.body),
    };
  };
}

const solicitacao = {
  cpf: '11122233396',
  nome: 'Fulano de Tal',
  email: 'fulano@exemplo.com.br',
  status: 'PENDENTE',
  motivo: null,
  _links: {
    self: { href: 'http://cliente:8080/solicitacoes/11122233396' },
    aprovacao: { href: 'http://cliente:8080/solicitacoes/11122233396/aprovacao' },
    rejeicao: { href: 'http://cliente:8080/solicitacoes/11122233396/rejeicao' },
  },
};

function bankFetch(): MsFetch {
  return mockFetch((url, init) => {
    if (url.endsWith('/auth/verificar')) {
      const body = JSON.parse(String(init.body ?? '{}')) as { email?: string };
      if (body.email === 'cli1@bantads.com.br') {
        return { status: 200, body: { cpf: CPF, tipo: 'CLIENTE' } };
      }
      if (body.email === 'ger1@bantads.com.br') {
        return { status: 200, body: { cpf: GERENTE, tipo: 'GERENTE' } };
      }
      return { status: 401, body: {} };
    }
    if (url.endsWith(`/clientes/${CPF}`) && init.method === 'GET') {
      return { status: 200, body: { cpf: CPF, nome: 'Catharyna', email: 'cli1@bantads.com.br' } };
    }
    if (url.endsWith(`/gerentes/${GERENTE}`) && (init.method === 'GET' || init.method === 'PUT')) {
      const extra =
        init.method === 'PUT' ? JSON.parse(String(init.body ?? '{}')) : { nome: 'Geniéve' };
      return {
        status: 200,
        body: {
          cpf: GERENTE,
          nome: extra.nome ?? 'Geniéve',
          email: 'ger1@bantads.com.br',
          telefone: extra.telefone ?? '41988880001',
          ativo: true,
          _links: { self: { href: 'http://gerente:8080/gerentes/' + GERENTE } },
        },
      };
    }
    if (url.includes('/solicitacoes') && init.method === 'POST' && url.endsWith('/solicitacoes')) {
      return {
        status: 201,
        location: '/solicitacoes/11122233396',
        body: solicitacao,
      };
    }
    if (url.includes('/solicitacoes/11122233396') && init.method === 'POST') {
      return {
        status: 200,
        body: {
          ...solicitacao,
          status: 'NAO_APROVADA',
          motivo: 'Renda incompatível',
          _links: { self: solicitacao._links.self },
        },
      };
    }
    if (url.includes('/solicitacoes')) {
      return {
        status: 200,
        body: {
          solicitacoes: [solicitacao],
          _links: { self: { href: 'http://cliente:8080/solicitacoes' } },
        },
      };
    }
    if (url.endsWith(`/clientes/${CPF}/conta`) || url.endsWith('/contas/1291')) {
      return {
        status: 200,
        body: {
          numero: '1291',
          cpfCliente: CPF,
          saldo: '800.00',
          dataCriacao: '2000-01-01',
          _links: {
            self: { href: 'http://conta:8080/contas/1291' },
            deposito: { href: 'http://conta:8080/contas/1291/deposito' },
          },
        },
      };
    }
    if (url.endsWith('/contas/1291/deposito') || url.endsWith('/contas/1291/saque')) {
      return {
        status: 201,
        body: {
          numeroConta: '1291',
          tipo: 'DEPOSITO',
          valor: '10.00',
          dataHora: '2026-04-30T10:00:00',
        },
      };
    }
    if (url.includes('/internal/contas/0950')) {
      return { status: 200, body: { numero: '0950', cpfCliente: OTHER } };
    }
    if (url.includes('/internal/contas/')) {
      return {
        status: 404,
        body: { status: 404, erro: 'Not Found', mensagem: 'Conta não encontrada' },
      };
    }
    if (url.includes('/clientes/nomes')) {
      return {
        status: 200,
        body: {
          clientes: [
            { cpf: CPF, nome: 'Catharyna', email: 'cli1@bantads.com.br' },
            { cpf: OTHER, nome: 'Cleuddônio', email: 'cli2@bantads.com.br' },
          ],
        },
      };
    }
    if (url.endsWith('/contas/1291/transferencia')) {
      const body = JSON.parse(String(init.body ?? '{}')) as {
        destino?: { numeroConta: string; cpf: string; nome: string };
      };
      return {
        status: 201,
        body: {
          numeroConta: '1291',
          tipo: 'TRANSFERENCIA',
          valor: '100.00',
          dataHora: '2026-04-30T10:00:00',
          destino: body.destino,
        },
      };
    }
    if (url.includes('/extrato')) {
      return {
        status: 200,
        body: {
          numeroConta: '1291',
          saldoAbertura: '800.00',
          movimentacoes: [],
          dataInicio: '2026-07-18',
          dataFim: '2026-08-17',
        },
      };
    }
    return { status: 404, body: { status: 404, erro: 'Not Found', mensagem: 'não encontrado' } };
  });
}

async function login(app: Awaited<ReturnType<typeof buildApp>>, email: string) {
  const response = await app.inject({
    method: 'POST',
    url: '/login',
    payload: { email, senha: 'tads' },
  });
  return (response.json() as { token: string }).token;
}

describe('F12 gateway sync flows', () => {
  it('T01 POST /solicitacoes is public with Location and pending links', async () => {
    const app = await buildApp({
      config: testConfig(),
      store: new MemoryStore(),
      fetchImpl: bankFetch(),
    });
    const response = await app.inject({
      method: 'POST',
      url: '/solicitacoes',
      payload: {
        cpf: '11122233396',
        nome: 'Fulano de Tal',
        email: 'fulano@exemplo.com.br',
        telefone: '41999990000',
        salario: '4500.00',
        endereco: {
          logradouro: 'Rua XV de Novembro',
          numero: '1299',
          cep: '80060000',
          cidade: 'Curitiba',
          uf: 'PR',
        },
      },
    });
    assert.equal(response.statusCode, 201);
    assert.equal(response.headers.location, '/solicitacoes/11122233396');
    const body = response.json() as {
      status: string;
      _links: { self: { href: string }; aprovacao: { href: string }; rejeicao: { href: string } };
    };
    assert.equal(body.status, 'PENDENTE');
    assert.ok(body._links.self.href.startsWith('http://localhost:3000/'));
    assert.ok(body._links.aprovacao.href.includes('/aprovacao'));
    assert.ok(body._links.rejeicao.href.includes('/rejeicao'));
    await app.close();
  });

  it('T03 GET own conta and T03p other cliente is 403', async () => {
    const app = await buildApp({
      config: testConfig(),
      store: new MemoryStore(),
      fetchImpl: bankFetch(),
    });
    const token = await login(app, 'cli1@bantads.com.br');
    const own = await app.inject({
      method: 'GET',
      url: `/clientes/${CPF}/conta`,
      headers: { 'x-access-token': token },
    });
    assert.equal(own.statusCode, 200);
    assert.equal((own.json() as { numero: string; saldo: string }).numero, '1291');
    assert.equal((own.json() as { saldo: string }).saldo, '800.00');
    const other = await app.inject({
      method: 'GET',
      url: `/clientes/${OTHER}/conta`,
      headers: { 'x-access-token': token },
    });
    assert.equal(other.statusCode, 403);
    await app.close();
  });

  it('T04 deposit returns 201 without saldo', async () => {
    const app = await buildApp({
      config: testConfig(),
      store: new MemoryStore(),
      fetchImpl: bankFetch(),
    });
    const token = await login(app, 'cli1@bantads.com.br');
    const response = await app.inject({
      method: 'POST',
      url: '/contas/1291/deposito',
      headers: { 'x-access-token': token },
      payload: { valor: '10.00' },
    });
    assert.equal(response.statusCode, 201);
    assert.equal((response.json() as { saldo?: string }).saldo, undefined);
    await app.close();
  });

  it('T06 enriches transfer and rejects same account or missing dest', async () => {
    const app = await buildApp({
      config: testConfig(),
      store: new MemoryStore(),
      fetchImpl: bankFetch(),
    });
    const token = await login(app, 'cli1@bantads.com.br');
    const same = await app.inject({
      method: 'POST',
      url: '/contas/1291/transferencia',
      headers: { 'x-access-token': token },
      payload: { contaDestino: '1291', valor: '100.00' },
    });
    assert.equal(same.statusCode, 422);
    const missing = await app.inject({
      method: 'POST',
      url: '/contas/1291/transferencia',
      headers: { 'x-access-token': token },
      payload: { contaDestino: '0001', valor: '100.00' },
    });
    assert.equal(missing.statusCode, 422);
    const ok = await app.inject({
      method: 'POST',
      url: '/contas/1291/transferencia',
      headers: { 'x-access-token': token },
      payload: { contaDestino: '0950', valor: '100.00' },
    });
    assert.equal(ok.statusCode, 201);
    const body = ok.json() as {
      tipo: string;
      destino: { numeroConta: string; nome: string };
      saldo?: string;
    };
    assert.equal(body.tipo, 'TRANSFERENCIA');
    assert.equal(body.destino.numeroConta, '0950');
    assert.equal(body.destino.nome, 'Cleuddônio');
    assert.equal(body.saldo, undefined);
    await app.close();
  });

  it('T08 GERENTE lists solicitacoes and T10 rejects', async () => {
    const app = await buildApp({
      config: testConfig(),
      store: new MemoryStore(),
      fetchImpl: bankFetch(),
    });
    const token = await login(app, 'ger1@bantads.com.br');
    const list = await app.inject({
      method: 'GET',
      url: '/solicitacoes',
      headers: { 'x-access-token': token },
    });
    assert.equal(list.statusCode, 200);
    const pendente = (
      list.json() as { solicitacoes: Array<{ status: string; _links: Record<string, unknown> }> }
    ).solicitacoes[0];
    assert.equal(pendente?.status, 'PENDENTE');
    assert.ok(pendente?._links.aprovacao);
    const rejeicao = await app.inject({
      method: 'POST',
      url: '/solicitacoes/11122233396/rejeicao',
      headers: { 'x-access-token': token },
      payload: { motivo: 'Renda incompatível' },
    });
    assert.equal(rejeicao.statusCode, 200);
    assert.equal((rejeicao.json() as { status: string; motivo: string }).status, 'NAO_APROVADA');
    await app.close();
  });

  it('T14 PUT gerente invalidates cache', async () => {
    let gets = 0;
    const fetchImpl = mockFetch((url, init) => {
      if (url.endsWith('/auth/verificar')) {
        return { status: 200, body: { cpf: GERENTE, tipo: 'GERENTE' } };
      }
      if (url.endsWith(`/gerentes/${GERENTE}`) && init.method === 'GET') {
        gets += 1;
        return {
          status: 200,
          body: {
            cpf: GERENTE,
            nome: gets === 1 ? 'Geniéve' : 'Geniéve Silva',
            email: 'ger1@bantads.com.br',
          },
        };
      }
      if (url.endsWith(`/gerentes/${GERENTE}`) && init.method === 'PUT') {
        return {
          status: 200,
          body: {
            cpf: GERENTE,
            nome: 'Geniéve Silva',
            email: 'ger1@bantads.com.br',
            telefone: '41988889999',
          },
        };
      }
      return { status: 404, body: {} };
    });
    const app = await buildApp({ config: testConfig(), store: new MemoryStore(), fetchImpl });
    const token = await login(app, 'ger1@bantads.com.br');
    gets = 0;
    const first = await app.inject({
      method: 'GET',
      url: `/gerentes/${GERENTE}`,
      headers: { 'x-access-token': token },
    });
    assert.equal((first.json() as { nome: string }).nome, 'Geniéve');
    const cached = await app.inject({
      method: 'GET',
      url: `/gerentes/${GERENTE}`,
      headers: { 'x-access-token': token },
    });
    assert.equal((cached.json() as { nome: string }).nome, 'Geniéve');
    assert.equal(gets, 1);
    await app.inject({
      method: 'PUT',
      url: `/gerentes/${GERENTE}`,
      headers: { 'x-access-token': token },
      payload: { nome: 'Geniéve Silva', telefone: '41988889999' },
    });
    const after = await app.inject({
      method: 'GET',
      url: `/gerentes/${GERENTE}`,
      headers: { 'x-access-token': token },
    });
    assert.equal((after.json() as { nome: string }).nome, 'Geniéve Silva');
    assert.equal(gets, 2);
    await app.close();
  });
});
