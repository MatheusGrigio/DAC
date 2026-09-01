import { Perfil } from '../types/enums.js';
import type { GatewayUser } from '../types/fastify.js';

export type Access =
  | { kind: 'public' }
  | { kind: 'auth' }
  | { kind: 'gerente' }
  | { kind: 'cliente' }
  | { kind: 'gerenteOrSelf' };

const PUBLIC: Array<{ method: string; path: string }> = [
  { method: 'GET', path: '/health' },
  { method: 'POST', path: '/login' },
  { method: 'POST', path: '/reboot' },
  { method: 'POST', path: '/solicitacoes' },
];

export function pathnameOf(url: string): string {
  const q = url.indexOf('?');
  return q === -1 ? url : url.slice(0, q);
}

export function isPublicRoute(method: string, path: string): boolean {
  return PUBLIC.some((route) => route.method === method && route.path === path);
}

export function accessFor(method: string, path: string): Access | { kind: 'unknown' } {
  if (isPublicRoute(method, path)) {
    return { kind: 'public' };
  }
  if (method === 'POST' && path === '/logout') {
    return { kind: 'auth' };
  }
  if (path === '/jobs' || path.startsWith('/jobs/')) {
    return { kind: 'auth' };
  }
  if (method === 'GET' && /^\/clientes\/\d{11}\/conta$/.test(path)) {
    return { kind: 'gerenteOrSelf' };
  }
  if (method === 'GET' && /^\/clientes\/\d{11}$/.test(path)) {
    return { kind: 'gerenteOrSelf' };
  }
  if (method === 'GET' && (path === '/clientes' || path.startsWith('/clientes?'))) {
    return { kind: 'gerente' };
  }
  if (path === '/solicitacoes' || path.startsWith('/solicitacoes/')) {
    return { kind: 'gerente' };
  }
  if (path === '/gerentes' || path.startsWith('/gerentes/')) {
    return { kind: 'gerente' };
  }
  if (path === '/relatorios/clientes' || path.startsWith('/relatorios/')) {
    return { kind: 'gerente' };
  }
  if (method === 'GET' && /^\/contas\/\d{4}$/.test(path)) {
    return { kind: 'gerenteOrSelf' };
  }
  if (method === 'GET' && /^\/contas\/\d{4}\/extrato$/.test(path)) {
    return { kind: 'cliente' };
  }
  if (method === 'POST' && /^\/contas\/\d{4}\/(deposito|saque|transferencia)$/.test(path)) {
    return { kind: 'cliente' };
  }
  return { kind: 'unknown' };
}

export function selfCpfFromPath(path: string): string | undefined {
  const cliente = path.match(/^\/clientes\/(\d{11})(?:\/conta)?$/);
  if (cliente) {
    return cliente[1];
  }
  return undefined;
}

export function isAllowed(access: Access, user: GatewayUser, path: string): boolean {
  switch (access.kind) {
    case 'public':
      return true;
    case 'auth':
      return true;
    case 'gerente':
      return user.tipo === Perfil.GERENTE;
    case 'cliente':
      return user.tipo === Perfil.CLIENTE;
    case 'gerenteOrSelf': {
      if (user.tipo === Perfil.GERENTE) {
        return true;
      }
      const cpf = selfCpfFromPath(path);
      return user.tipo === Perfil.CLIENTE && (!cpf || cpf === user.cpf);
    }
  }
}
