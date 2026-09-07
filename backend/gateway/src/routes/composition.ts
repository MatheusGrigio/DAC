import type { FastifyReply, FastifyRequest } from 'fastify';
import type { AppConfig } from '../config.js';
import { applyHateoas, rewriteLinks } from '../http/hateoas.js';
import { msRequest, type MsFetch, type MsResponse } from '../http/ms-client.js';
import { sortByNome } from '../http/pt-br.js';
import type { KeyValueStore } from '../redis/store.js';
import { DINHEIRO_PATTERN } from '../types/patterns.js';

export type CompositionDeps = {
  config: AppConfig;
  store: KeyValueStore;
  fetchImpl?: MsFetch;
};

export function identityHeadersFrom(user?: { cpf: string; tipo: string }): Record<string, string> {
  const headers: Record<string, string> = { accept: 'application/json' };
  if (user) {
    headers['X-User-CPF'] = user.cpf;
    headers['X-User-Tipo'] = user.tipo;
  }
  return headers;
}

function identityHeaders(request: FastifyRequest): Record<string, string> {
  return identityHeadersFrom(request.user);
}

function asRecord(value: unknown): Record<string, unknown> | null {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    return null;
  }
  return value as Record<string, unknown>;
}

function textField(source: unknown, key: string): string | undefined {
  const record = asRecord(source);
  if (!record) {
    return undefined;
  }
  const value = record[key];
  return typeof value === 'string' && value.length > 0 ? value : undefined;
}

function dinheiro(value: unknown): string | undefined {
  if (typeof value === 'string' && DINHEIRO_PATTERN.test(value)) {
    return value;
  }
  if (typeof value === 'number' && Number.isFinite(value)) {
    return value.toFixed(2);
  }
  return undefined;
}

function sendMs(reply: FastifyReply, forwarded: MsResponse, publicUrl: string): void {
  const body =
    forwarded.body && typeof forwarded.body === 'object'
      ? rewriteLinks(forwarded.body, publicUrl)
      : forwarded.body;
  reply.code(forwarded.status).send(body);
}

export function composeClientes(lista: unknown, saldos: unknown, publicUrl: string): unknown {
  const root = asRecord(lista) ?? {};
  const rows = Array.isArray(root.clientes) ? root.clientes : [];
  const saldoMap = asRecord(saldos) ?? {};
  const clientes = sortByNome(
    rows.flatMap((row) => {
      const item = asRecord(row);
      if (!item) {
        return [];
      }
      const cpf = textField(item, 'cpf');
      const nome = textField(item, 'nome');
      const endereco = asRecord(item.endereco);
      const cidade = textField(endereco, 'cidade');
      const estado = textField(endereco, 'uf');
      const saldo = dinheiro(asRecord(saldoMap[cpf ?? ''])?.saldo);
      if (!cpf || !nome || !cidade || !estado || !saldo) {
        return [];
      }
      return [
        {
          cpf,
          nome,
          cidade,
          estado,
          saldo,
          _links: {
            self: { href: `${publicUrl}/clientes/${cpf}` },
            conta: { href: `${publicUrl}/clientes/${cpf}/conta` },
          },
        },
      ];
    }),
  );
  return rewriteLinks({ clientes, _links: root._links }, publicUrl);
}

export type RelatorioClienteLinha = {
  cpf: string;
  nome: string;
  email: string;
  salario: string;
  numeroConta: string;
  saldo: string;
  cpfGerente: string;
  nomeGerente: string;
};

export function composeRelatorioClientes(
  lista: unknown,
  saldos: unknown,
  gerentes: unknown,
): { clientes: RelatorioClienteLinha[] } {
  const root = asRecord(lista);
  const rows = Array.isArray(root?.clientes) ? (root.clientes as unknown[]) : [];
  const saldoMap = asRecord(saldos) ?? {};
  const gerenteRoot = asRecord(gerentes);
  const gerenteRows = Array.isArray(gerenteRoot?.gerentes)
    ? (gerenteRoot.gerentes as unknown[])
    : [];
  const nomesGerente = new Map<string, string>();
  for (const row of gerenteRows) {
    const item = asRecord(row);
    const cpf = textField(item, 'cpf');
    const nome = textField(item, 'nome');
    if (cpf && nome) {
      nomesGerente.set(cpf, nome);
    }
  }
  const clientes = sortByNome(
    rows.flatMap((row) => {
      const item = asRecord(row);
      const conta = asRecord(saldoMap[textField(item, 'cpf') ?? '']);
      const cpf = textField(item, 'cpf');
      const nome = textField(item, 'nome');
      const email = textField(item, 'email');
      const salario = dinheiro(item === null ? undefined : item.salario);
      const numeroConta = textField(conta, 'numero');
      const saldo = dinheiro(conta === null ? undefined : conta.saldo);
      const cpfGerente = textField(conta, 'cpfGerente');
      const nomeGerente = cpfGerente ? nomesGerente.get(cpfGerente) : undefined;
      if (
        !cpf ||
        !nome ||
        !email ||
        !salario ||
        !numeroConta ||
        !saldo ||
        !cpfGerente ||
        !nomeGerente
      ) {
        return [];
      }
      return [
        {
          cpf,
          nome,
          email,
          salario,
          numeroConta,
          saldo,
          cpfGerente,
          nomeGerente,
        },
      ];
    }),
  );
  return { clientes };
}

export async function collectRelatorioClientes(
  deps: CompositionDeps,
  headers: Record<string, string>,
): Promise<
  { ok: true; body: { clientes: RelatorioClienteLinha[] } } | { ok: false; erro: string }
> {
  const [lista, saldos, gerentes] = await Promise.all([
    msRequest({
      baseUrl: deps.config.clienteUrl,
      method: 'GET',
      path: '/clientes',
      headers,
      fetchImpl: deps.fetchImpl,
    }),
    msRequest({
      baseUrl: deps.config.contaUrl,
      method: 'GET',
      path: '/internal/saldos',
      headers,
      fetchImpl: deps.fetchImpl,
    }),
    msRequest({
      baseUrl: deps.config.gerenteUrl,
      method: 'GET',
      path: '/gerentes',
      headers,
      fetchImpl: deps.fetchImpl,
    }),
  ]);
  if (lista.status !== 200 || saldos.status !== 200 || gerentes.status !== 200) {
    return { ok: false, erro: 'Falha ao montar o relatório de clientes' };
  }
  return { ok: true, body: composeRelatorioClientes(lista.body, saldos.body, gerentes.body) };
}

export function composeGerentes(lista: unknown, contagens: unknown, publicUrl: string): unknown {
  const root = asRecord(lista) ?? {};
  const rows = Array.isArray(root.gerentes) ? root.gerentes : [];
  const counts = asRecord(contagens) ?? {};
  const gerentes = sortByNome(
    rows.flatMap((row) => {
      const item = asRecord(row);
      if (!item) {
        return [];
      }
      const cpf = textField(item, 'cpf');
      const nome = textField(item, 'nome');
      if (!cpf || !nome) {
        return [];
      }
      const raw = counts[cpf];
      const quantidadeClientes = typeof raw === 'number' && Number.isInteger(raw) ? raw : 0;
      return [{ ...item, cpf, nome, quantidadeClientes }];
    }),
  );
  return rewriteLinks({ gerentes, _links: root._links }, publicUrl);
}

export async function listarClientes(
  request: FastifyRequest,
  reply: FastifyReply,
  deps: CompositionDeps,
): Promise<void> {
  const path = request.url.startsWith('/') ? request.url : `/${request.url}`;
  const [lista, saldos] = await Promise.all([
    msRequest({
      baseUrl: deps.config.clienteUrl,
      method: 'GET',
      path,
      headers: identityHeaders(request),
      fetchImpl: deps.fetchImpl,
    }),
    msRequest({
      baseUrl: deps.config.contaUrl,
      method: 'GET',
      path: '/internal/saldos',
      headers: identityHeaders(request),
      fetchImpl: deps.fetchImpl,
    }),
  ]);
  if (lista.status !== 200) {
    sendMs(reply, lista, deps.config.publicUrl);
    return;
  }
  if (saldos.status !== 200) {
    sendMs(reply, saldos, deps.config.publicUrl);
    return;
  }
  reply.code(200).send(
    applyHateoas(composeClientes(lista.body, saldos.body, deps.config.publicUrl), {
      publicUrl: deps.config.publicUrl,
      user: request.user,
      requestUrl: path,
    }),
  );
}

export async function listarGerentes(
  request: FastifyRequest,
  reply: FastifyReply,
  deps: CompositionDeps,
): Promise<void> {
  const [lista, contagens] = await Promise.all([
    msRequest({
      baseUrl: deps.config.gerenteUrl,
      method: 'GET',
      path: '/gerentes',
      headers: identityHeaders(request),
      fetchImpl: deps.fetchImpl,
    }),
    msRequest({
      baseUrl: deps.config.contaUrl,
      method: 'GET',
      path: '/internal/contagem-por-gerente',
      headers: identityHeaders(request),
      fetchImpl: deps.fetchImpl,
    }),
  ]);
  if (lista.status !== 200) {
    sendMs(reply, lista, deps.config.publicUrl);
    return;
  }
  if (contagens.status !== 200) {
    sendMs(reply, contagens, deps.config.publicUrl);
    return;
  }
  reply.code(200).send(
    applyHateoas(composeGerentes(lista.body, contagens.body, deps.config.publicUrl), {
      publicUrl: deps.config.publicUrl,
      user: request.user,
      requestUrl: '/gerentes',
    }),
  );
}
