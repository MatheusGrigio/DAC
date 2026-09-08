import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import { buildApp } from '../src/app.ts';
import { loadConfig } from '../src/config.ts';
import { sortByNome } from '../src/http/pt-br.ts';
import {
  composeClientes,
  composeGerentes,
  composeRelatorioClientes,
} from '../src/routes/composition.ts';
import type { MsFetch } from '../src/http/ms-client.ts';
import { MemoryStore } from './memory-store.ts';

const GERENTE = '98574307084';
const PUBLIC = 'http://localhost:3000';

function testConfig() {
  return loadConfig({
    JWT_SECRET: 'test-secret-f13',
    GATEWAY_PUBLIC_URL: PUBLIC,
    CORS_ORIGIN: 'http://localhost:4200',
    AUTH_URL: 'http://auth:8080',
    CLIENTE_URL: 'http://cliente:8080',
    GERENTE_URL: 'http://gerente:8080',
    CONTA_URL: 'http://conta:8080',
  });
}

function mockFetch(
  handler: (url: string, init: RequestInit) => { status: number; body: unknown },
): MsFetch {
  return async (url, init) => {
    const result = handler(url, init);
    return {
      status: result.status,
      headers: new Headers({ 'content-type': 'application/json' }),
      text: async () => JSON.stringify(result.body),
    };
  };
}

describe('F13 composition', () => {
  it('sorts seed names with pt-BR base sensitivity', () => {
    const nomes = sortByNome(
      ['Cutardo', 'Catharyna', 'Coândrya', 'Cleuddônio', 'Catianna'].map((nome) => ({ nome })),
    ).map((item) => item.nome);
    assert.deepEqual(nomes, ['Catharyna', 'Catianna', 'Cleuddônio', 'Coândrya', 'Cutardo']);
  });

  it('T11 joins saldo and maps cidade/estado', () => {
    const composed = composeClientes(
      {
        clientes: [
          {
            cpf: '12912861012',
            nome: 'Catharyna',
            endereco: { cidade: 'Curitiba', uf: 'PR' },
            _links: { self: { href: 'http://cliente:8080/clientes/12912861012' } },
          },
          {
            cpf: '85733854057',
            nome: 'Catianna',
            endereco: { cidade: 'Curitiba', uf: 'PR' },
            _links: { self: { href: 'http://cliente:8080/clientes/85733854057' } },
          },
          {
            cpf: '09506382000',
            nome: 'Cleuddônio',
            endereco: { cidade: 'Curitiba', uf: 'PR' },
            _links: { self: { href: 'http://cliente:8080/clientes/09506382000' } },
          },
        ],
        _links: { self: { href: 'http://cliente:8080/clientes' } },
      },
      {
        '12912861012': { saldo: '800.00', numero: '1291' },
        '85733854057': { saldo: '200.00', numero: '8573' },
      },
      PUBLIC,
    ) as {
      clientes: Array<{
        nome: string;
        saldo: string;
        estado: string;
        _links: { self: { href: string } };
      }>;
    };
    assert.deepEqual(
      composed.clientes.map((item) => item.nome),
      ['Catharyna', 'Catianna'],
    );
    assert.equal(composed.clientes[1]?.saldo, '200.00');
    assert.equal(composed.clientes[1]?.estado, 'PR');
    assert.equal(composed.clientes[1]?._links.self.href, `${PUBLIC}/clientes/85733854057`);
  });

  it('T12 fills quantidadeClientes including zero', () => {
    const composed = composeGerentes(
      {
        gerentes: [
          { cpf: '40501740066', nome: 'Gadamântio', ativo: true },
          { cpf: GERENTE, nome: 'Geniéve', ativo: true },
          { cpf: '64065268052', nome: 'Godophredo', ativo: true },
          { cpf: '23862179060', nome: 'Gyândula', ativo: true },
        ],
        _links: { self: { href: 'http://gerente:8080/gerentes' } },
      },
      { [GERENTE]: 2, '64065268052': 2, '23862179060': 1 },
      PUBLIC,
    ) as { gerentes: Array<{ nome: string; quantidadeClientes: number }> };
    assert.deepEqual(
      composed.gerentes.map((item) => [item.nome, item.quantidadeClientes]),
      [
        ['Gadamântio', 0],
        ['Geniéve', 2],
        ['Godophredo', 2],
        ['Gyândula', 1],
      ],
    );
  });

  it('GET /clientes?busca=Cat is GERENTE composition', async () => {
    const fetchImpl = mockFetch((url) => {
      if (url.endsWith('/auth/verificar')) {
        return { status: 200, body: { cpf: GERENTE, tipo: 'GERENTE' } };
      }
      if (url.endsWith(`/gerentes/${GERENTE}`)) {
        return {
          status: 200,
          body: { cpf: GERENTE, nome: 'Geniéve', email: 'ger1@bantads.com.br' },
        };
      }
      if (url.includes('/clientes?busca=Cat')) {
        return {
          status: 200,
          body: {
            clientes: [
              {
                cpf: '12912861012',
                nome: 'Catharyna',
                endereco: { cidade: 'Curitiba', uf: 'PR' },
                _links: { self: { href: 'http://cliente:8080/clientes/12912861012' } },
              },
              {
                cpf: '85733854057',
                nome: 'Catianna',
                endereco: { cidade: 'Curitiba', uf: 'PR' },
                _links: { self: { href: 'http://cliente:8080/clientes/85733854057' } },
              },
            ],
            _links: { self: { href: 'http://cliente:8080/clientes' } },
          },
        };
      }
      if (url.endsWith('/internal/saldos')) {
        return {
          status: 200,
          body: {
            '12912861012': { saldo: '800.00' },
            '85733854057': { saldo: '200.00' },
          },
        };
      }
      return { status: 404, body: {} };
    });
    const app = await buildApp({ config: testConfig(), store: new MemoryStore(), fetchImpl });
    const login = await app.inject({
      method: 'POST',
      url: '/login',
      payload: { email: 'ger1@bantads.com.br', senha: 'tads' },
    });
    const token = (login.json() as { token: string }).token;
    const response = await app.inject({
      method: 'GET',
      url: '/clientes?busca=Cat',
      headers: { 'x-access-token': token },
    });
    assert.equal(response.statusCode, 200);
    const body = response.json() as {
      clientes: Array<{
        nome: string;
        saldo: string;
        _links: { self: { href: string }; conta: { href: string } };
      }>;
      _links: { self: { href: string } };
    };
    assert.deepEqual(
      body.clientes.map((item) => item.nome),
      ['Catharyna', 'Catianna'],
    );
    assert.ok(body.clientes.every((item) => /^\d+\.\d{2}$/.test(item.saldo)));
    assert.ok(body._links.self.href.includes('busca=Cat'));
    assert.ok(body.clientes[0]?._links.conta.href.endsWith('/clientes/12912861012/conta'));
    await app.close();
  });

  it('T16 joins conta and gerente and sorts by nome', () => {
    const composed = composeRelatorioClientes(
      {
        clientes: [
          {
            cpf: '58872160006',
            nome: 'Cutardo',
            email: 'cli4@bantads.com.br',
            salario: '500.00',
          },
          {
            cpf: '12912861012',
            nome: 'Catharyna',
            email: 'cli1@bantads.com.br',
            salario: '10000.00',
          },
          {
            cpf: '76179646090',
            nome: 'Coândrya',
            email: 'cli5@bantads.com.br',
            salario: '1500.00',
          },
        ],
      },
      {
        '12912861012': { saldo: '800.00', numero: '1291', cpfGerente: '98574307084' },
        '58872160006': { saldo: '150000.00', numero: '5887', cpfGerente: '98574307084' },
        '76179646090': { saldo: '1500.00', numero: '7617', cpfGerente: '64065268052' },
      },
      {
        gerentes: [
          { cpf: '98574307084', nome: 'Geniéve' },
          { cpf: '64065268052', nome: 'Godophredo' },
        ],
      },
    );
    assert.deepEqual(
      composed.clientes.map((item) => item.nome),
      ['Catharyna', 'Coândrya', 'Cutardo'],
    );
    assert.equal(composed.clientes[0]?.numeroConta, '1291');
    assert.equal(composed.clientes[0]?.nomeGerente, 'Geniéve');
    assert.equal((composed as { _links?: unknown })._links, undefined);
  });
});
