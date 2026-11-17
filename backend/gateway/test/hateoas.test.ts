import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import { applyHateoas, cacheableCadastro, rewriteLinks } from '../src/http/hateoas.ts';
import { buildApp } from '../src/app.ts';
import { loadConfig } from '../src/config.ts';
import { clienteCacheKey } from '../src/redis/cache.ts';
import type { MsFetch } from '../src/http/ms-client.ts';
import { MemoryStore } from './memory-store.ts';

const PUBLIC = 'http://localhost:3000';
const CPF = '12912861012';
const GERENTE = '98574307084';
const OTHER = '64065268052';

describe('F18 HATEOAS and cache', () => {
  it('walks nested hrefs to the gateway public URL', () => {
    const rewritten = rewriteLinks(
      {
        clientes: [{ _links: { self: { href: 'http://cliente:8080/clientes/1' } } }],
        _links: { self: { href: 'http://cliente:8080/clientes?busca=Cat' } },
      },
      PUBLIC,
    ) as {
      _links: { self: { href: string } };
      clientes: Array<{ _links: { self: { href: string } } }>;
    };
    assert.equal(rewritten._links.self.href, `${PUBLIC}/clientes?busca=Cat`);
    assert.equal(rewritten.clientes[0]?._links.self.href, `${PUBLIC}/clientes/1`);
  });

  it('omits remocao for the authenticated gerente and write rels for GERENTE on conta', () => {
    const gerente = applyHateoas(
      {
        cpf: GERENTE,
        nome: 'Geniéve',
        telefone: '41988880001',
        ativo: true,
        _links: {
          self: { href: `http://gerente:8080/gerentes/${GERENTE}` },
          remocao: { href: `http://gerente:8080/gerentes/${GERENTE}` },
        },
      },
      { publicUrl: PUBLIC, user: { cpf: GERENTE, tipo: 'GERENTE' } },
    ) as { _links: Record<string, unknown> };
    assert.equal(gerente._links.remocao, undefined);
    assert.ok(gerente._links.atualizacao);

    const other = applyHateoas(
      {
        cpf: OTHER,
        nome: 'Godophredo',
        telefone: '41988880002',
        ativo: true,
        _links: { self: { href: `http://gerente:8080/gerentes/${OTHER}` } },
      },
      { publicUrl: PUBLIC, user: { cpf: GERENTE, tipo: 'GERENTE' } },
    ) as { _links: { remocao: { href: string } } };
    assert.equal(other._links.remocao.href, `${PUBLIC}/gerentes/${OTHER}`);

    const conta = applyHateoas(
      {
        numero: '1291',
        cpfCliente: CPF,
        cpfGerente: GERENTE,
        saldo: '800.00',
        dataCriacao: '2000-01-01',
        _links: {
          self: { href: 'http://conta:8080/contas/1291' },
          cliente: { href: `http://conta:8080/clientes/${CPF}` },
          deposito: { href: 'http://conta:8080/contas/1291/deposito' },
          saque: { href: 'http://conta:8080/contas/1291/saque' },
          transferencia: { href: 'http://conta:8080/contas/1291/transferencia' },
          extrato: { href: 'http://conta:8080/contas/1291/extrato' },
        },
      },
      { publicUrl: PUBLIC, user: { cpf: GERENTE, tipo: 'GERENTE' } },
    ) as { _links: Record<string, unknown> };
    assert.ok(conta._links.self);
    assert.ok(conta._links.cliente);
    assert.equal(conta._links.deposito, undefined);
    assert.equal(conta._links.extrato, undefined);
  });

  it('keeps remocao in cache and strips it per viewer', () => {
    const stored = cacheableCadastro(
      {
        cpf: GERENTE,
        nome: 'Geniéve',
        telefone: '41988880001',
        ativo: true,
        _links: { self: { href: `http://gerente:8080/gerentes/${GERENTE}` } },
      },
      PUBLIC,
    ) as { _links: { remocao: { href: string } } };
    assert.equal(stored._links.remocao.href, `${PUBLIC}/gerentes/${GERENTE}`);
    const viewed = applyHateoas(stored, {
      publicUrl: PUBLIC,
      user: { cpf: GERENTE, tipo: 'GERENTE' },
    }) as { _links: Record<string, unknown> };
    assert.equal(viewed._links.remocao, undefined);
  });

  it('does not cache GET cliente 404', async () => {
    let gets = 0;
    const fetchImpl: MsFetch = async (url) => {
      const json = (body: unknown, status = 200) => ({
        status,
        headers: new Headers({ 'content-type': 'application/json' }),
        text: async () => JSON.stringify(body),
      });
      if (url.endsWith('/auth/verificar')) {
        return json({ cpf: GERENTE, tipo: 'GERENTE' });
      }
      if (url.endsWith(`/gerentes/${GERENTE}`)) {
        return json({ cpf: GERENTE, nome: 'Geniéve', email: 'ger1@bantads.com.br' });
      }
      if (url.endsWith('/clientes/00000000000')) {
        gets += 1;
        return json({ status: 404, erro: 'Not Found', mensagem: 'Cliente não encontrado' }, 404);
      }
      return json({}, 404);
    };
    const store = new MemoryStore();
    const app = await buildApp({
      config: loadConfig({
        JWT_SECRET: 'test-secret-f18',
        GATEWAY_PUBLIC_URL: PUBLIC,
        CORS_ORIGIN: 'http://localhost:4200',
        AUTH_URL: 'http://auth:8080',
        CLIENTE_URL: 'http://cliente:8080',
        GERENTE_URL: 'http://gerente:8080',
        CONTA_URL: 'http://conta:8080',
      }),
      store,
      fetchImpl,
    });
    const login = await app.inject({
      method: 'POST',
      url: '/login',
      payload: { email: 'ger1@bantads.com.br', senha: 'tads' },
    });
    const token = (login.json() as { token: string }).token;
    const first = await app.inject({
      method: 'GET',
      url: '/clientes/00000000000',
      headers: { 'x-access-token': token },
    });
    assert.equal(first.statusCode, 404);
    const second = await app.inject({
      method: 'GET',
      url: '/clientes/00000000000',
      headers: { 'x-access-token': token },
    });
    assert.equal(second.statusCode, 404);
    assert.equal(gets, 2);
    assert.equal(await store.get(clienteCacheKey('00000000000')), null);
    await app.close();
  });
});
