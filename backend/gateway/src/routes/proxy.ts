import type { FastifyInstance, FastifyReply, FastifyRequest } from 'fastify';
import { type AppConfig } from '../config.js';
import { Erros } from '../http/errors.js';
import { applyHateoas, cacheableCadastro, rewriteLocation } from '../http/hateoas.js';
import { msRequest, type MsFetch, type MsResponse } from '../http/ms-client.js';
import { clienteCacheKey, gerenteCacheKey, readCache, writeCache } from '../redis/cache.js';
import type { KeyValueStore } from '../redis/store.js';
import { transferenciaInputSchema } from '../types/schemas.js';
import { listarClientes, listarGerentes } from './composition.js';

type ProxyDeps = { config: AppConfig; store: KeyValueStore; fetchImpl?: MsFetch };

function identityHeaders(request: FastifyRequest): Record<string, string> {
  const user = request.user;
  const headers: Record<string, string> = { accept: 'application/json' };
  if (user) {
    headers['X-User-CPF'] = user.cpf;
    headers['X-User-Tipo'] = user.tipo;
  }
  if (typeof request.headers['content-type'] === 'string') {
    headers['content-type'] = request.headers['content-type'];
  }
  return headers;
}

function textField(source: unknown, key: string): string | undefined {
  if (!source || typeof source !== 'object') {
    return undefined;
  }
  const value = (source as Record<string, unknown>)[key];
  return typeof value === 'string' && value.length > 0 ? value : undefined;
}

async function forward(
  request: FastifyRequest,
  deps: ProxyDeps,
  baseUrl: string,
  path?: string,
  body?: unknown,
): Promise<MsResponse> {
  const targetPath = path ?? (request.url.startsWith('/') ? request.url : `/${request.url}`);
  return msRequest({
    baseUrl,
    method: request.method,
    path: targetPath,
    headers: identityHeaders(request),
    body: body !== undefined ? body : request.body,
    fetchImpl: deps.fetchImpl,
  });
}

function sendForwarded(
  reply: FastifyReply,
  forwarded: MsResponse,
  request: FastifyRequest,
  publicUrl: string,
): unknown {
  const location = rewriteLocation(forwarded.location, publicUrl);
  if (location) {
    reply.header('Location', location);
  }
  let body = forwarded.body;
  if (forwarded.status < 400 && body && typeof body === 'object') {
    body = applyHateoas(body, {
      publicUrl,
      user: request.user,
      requestUrl: request.url.startsWith('/') ? request.url : `/${request.url}`,
    });
  }
  reply.code(forwarded.status).send(body);
  return body;
}

async function proxy(
  request: FastifyRequest,
  reply: FastifyReply,
  deps: ProxyDeps,
  baseUrl: string,
): Promise<void> {
  const forwarded = await forward(request, deps, baseUrl);
  sendForwarded(reply, forwarded, request, deps.config.publicUrl);
}

async function cachedGet(
  request: FastifyRequest,
  reply: FastifyReply,
  deps: ProxyDeps,
  baseUrl: string,
  cacheKey: string,
): Promise<void> {
  const hit = await readCache(deps.store, cacheKey);
  if (hit) {
    reply.code(200).send(
      applyHateoas(hit, {
        publicUrl: deps.config.publicUrl,
        user: request.user,
        requestUrl: request.url.startsWith('/') ? request.url : `/${request.url}`,
      }),
    );
    return;
  }
  const forwarded = await forward(request, deps, baseUrl);
  if (forwarded.status !== 200) {
    sendForwarded(reply, forwarded, request, deps.config.publicUrl);
    return;
  }
  const stored = cacheableCadastro(forwarded.body, deps.config.publicUrl);
  await writeCache(deps.store, cacheKey, stored);
  reply.code(200).send(
    applyHateoas(stored, {
      publicUrl: deps.config.publicUrl,
      user: request.user,
      requestUrl: request.url.startsWith('/') ? request.url : `/${request.url}`,
    }),
  );
}

function nomePorCpf(lista: unknown, cpf: string): string | undefined {
  if (!lista || typeof lista !== 'object') {
    return undefined;
  }
  const clientes = (lista as { clientes?: unknown }).clientes;
  if (!Array.isArray(clientes)) {
    return undefined;
  }
  const found = clientes.find((item) => textField(item, 'cpf') === cpf);
  return textField(found, 'nome');
}

async function transferir(
  request: FastifyRequest,
  reply: FastifyReply,
  deps: ProxyDeps,
): Promise<void> {
  const numero = (request.params as { numero: string }).numero;
  const parsed = transferenciaInputSchema.safeParse(request.body);
  if (!parsed.success) {
    return reply.code(400).send(Erros.badRequest('Requisição malformada'));
  }
  if (parsed.data.contaDestino === numero) {
    return reply
      .code(422)
      .send(Erros.unprocessable('Não é permitido transferir para a própria conta'));
  }
  const user = request.user;
  if (!user) {
    return reply.code(401).send(Erros.forbidden());
  }
  const destino = await msRequest({
    baseUrl: deps.config.contaUrl,
    method: 'GET',
    path: `/internal/contas/${parsed.data.contaDestino}`,
    headers: identityHeaders(request),
    fetchImpl: deps.fetchImpl,
  });
  if (destino.status === 404) {
    return reply.code(422).send(Erros.unprocessable('Conta destino inexistente'));
  }
  if (destino.status !== 200) {
    sendForwarded(reply, destino, request, deps.config.publicUrl);
    return;
  }
  const cpfDestino = textField(destino.body, 'cpfCliente');
  if (!cpfDestino) {
    return reply.code(422).send(Erros.unprocessable('Conta destino inexistente'));
  }
  const nomes = await msRequest({
    baseUrl: deps.config.clienteUrl,
    method: 'GET',
    path: `/clientes/nomes?cpfs=${user.cpf},${cpfDestino}`,
    headers: identityHeaders(request),
    fetchImpl: deps.fetchImpl,
  });
  const nomeOrigem = nomePorCpf(nomes.body, user.cpf);
  const nomeDestino = nomePorCpf(nomes.body, cpfDestino);
  if (!nomeOrigem || !nomeDestino) {
    return reply.code(422).send(Erros.unprocessable('Conta destino inexistente'));
  }
  const command = await msRequest({
    baseUrl: deps.config.contaUrl,
    method: 'POST',
    path: `/contas/${numero}/transferencia`,
    headers: identityHeaders(request),
    body: {
      valor: parsed.data.valor,
      origem: { numeroConta: numero, cpf: user.cpf, nome: nomeOrigem },
      destino: { numeroConta: parsed.data.contaDestino, cpf: cpfDestino, nome: nomeDestino },
    },
    fetchImpl: deps.fetchImpl,
  });
  sendForwarded(reply, command, request, deps.config.publicUrl);
}

export function registerProxy(app: FastifyInstance, deps: ProxyDeps): void {
  const cliente = (request: FastifyRequest, reply: FastifyReply) =>
    proxy(request, reply, deps, deps.config.clienteUrl);
  const gerente = (request: FastifyRequest, reply: FastifyReply) =>
    proxy(request, reply, deps, deps.config.gerenteUrl);
  const conta = (request: FastifyRequest, reply: FastifyReply) =>
    proxy(request, reply, deps, deps.config.contaUrl);

  app.post('/solicitacoes', cliente);
  app.get('/solicitacoes', cliente);
  app.get('/solicitacoes/:cpf', cliente);
  app.post('/solicitacoes/:cpf/rejeicao', cliente);

  app.get('/clientes', (request, reply) => listarClientes(request, reply, deps));
  app.get('/clientes/:cpf', async (request, reply) => {
    const cpf = (request.params as { cpf: string }).cpf;
    await cachedGet(request, reply, deps, deps.config.clienteUrl, clienteCacheKey(cpf));
  });

  app.get('/clientes/:cpf/conta', conta);
  app.get('/contas/:numero', conta);
  app.get('/contas/:numero/extrato', conta);
  app.post('/contas/:numero/deposito', conta);
  app.post('/contas/:numero/saque', conta);
  app.post('/contas/:numero/transferencia', (request, reply) => transferir(request, reply, deps));

  app.get('/gerentes', (request, reply) => listarGerentes(request, reply, deps));
  app.get('/gerentes/:cpf', async (request, reply) => {
    const cpf = (request.params as { cpf: string }).cpf;
    await cachedGet(request, reply, deps, deps.config.gerenteUrl, gerenteCacheKey(cpf));
  });
  app.put('/gerentes/:cpf', async (request, reply) => {
    const cpf = (request.params as { cpf: string }).cpf;
    const forwarded = await forward(request, deps, deps.config.gerenteUrl);
    if (forwarded.status === 200) {
      await deps.store.del(gerenteCacheKey(cpf));
    }
    sendForwarded(reply, forwarded, request, deps.config.publicUrl);
  });
}
