import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import {
  accessFor,
  isAllowed,
  isPublicRoute,
  pathnameOf,
  selfCpfFromPath,
} from '../src/auth/acl.ts';
import { Perfil } from '../src/types/enums.ts';
import type { GatewayUser } from '../src/types/fastify.ts';

const CLIENTE_CPF = '12912861012';
const cliente: GatewayUser = { cpf: CLIENTE_CPF, tipo: Perfil.CLIENTE, jti: 'a', exp: 0 };
const outroCliente: GatewayUser = { cpf: '98574307084', tipo: Perfil.CLIENTE, jti: 'b', exp: 0 };
const gerente: GatewayUser = { cpf: '64065268052', tipo: Perfil.GERENTE, jti: 'c', exp: 0 };

describe('E03 ACL', () => {
  it('mantém a lista de rotas públicas exigida (health, login, reboot, solicitacoes)', () => {
    assert.equal(isPublicRoute('GET', '/health'), true);
    assert.equal(isPublicRoute('POST', '/login'), true);
    assert.equal(isPublicRoute('POST', '/reboot'), true);
    assert.equal(isPublicRoute('POST', '/solicitacoes'), true);
    assert.equal(isPublicRoute('POST', '/logout'), false);
  });

  it('ignora a querystring ao resolver o pathname', () => {
    assert.equal(pathnameOf('/clientes?busca=Cat'), '/clientes');
    assert.equal(pathnameOf('/health'), '/health');
  });

  it('classifica /logout e /jobs como somente autenticado, sem exigir perfil', () => {
    assert.deepEqual(accessFor('POST', '/logout'), { kind: 'auth' });
    assert.deepEqual(accessFor('GET', '/jobs/123'), { kind: 'auth' });
  });

  it('restringe listagem e cadastro de gerentes ao perfil GERENTE', () => {
    assert.deepEqual(accessFor('GET', '/clientes'), { kind: 'gerente' });
    assert.deepEqual(accessFor('POST', '/gerentes'), { kind: 'gerente' });
    assert.equal(isAllowed({ kind: 'gerente' }, cliente, '/clientes'), false);
    assert.equal(isAllowed({ kind: 'gerente' }, gerente, '/clientes'), true);
  });

  it('permite operações de conta (saque/depósito/extrato) apenas ao CLIENTE', () => {
    assert.deepEqual(accessFor('POST', '/contas/1234/saque'), { kind: 'cliente' });
    assert.deepEqual(accessFor('GET', '/contas/1234/extrato'), { kind: 'cliente' });
    assert.equal(isAllowed({ kind: 'cliente' }, gerente, '/contas/1234/saque'), false);
    assert.equal(isAllowed({ kind: 'cliente' }, cliente, '/contas/1234/saque'), true);
  });

  it('gerenteOrSelf: gerente sempre passa; cliente só acessa o próprio CPF', () => {
    const path = `/clientes/${CLIENTE_CPF}`;
    assert.equal(selfCpfFromPath(path), CLIENTE_CPF);
    assert.equal(isAllowed({ kind: 'gerenteOrSelf' }, gerente, path), true);
    assert.equal(isAllowed({ kind: 'gerenteOrSelf' }, cliente, path), true);
    assert.equal(isAllowed({ kind: 'gerenteOrSelf' }, outroCliente, path), false);
  });

  it('rotas não mapeadas retornam unknown (o hook deve responder 404)', () => {
    assert.deepEqual(accessFor('DELETE', '/rota-inexistente'), { kind: 'unknown' });
  });
});