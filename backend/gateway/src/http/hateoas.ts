const INTERNAL_HOSTS = new Set([
  'auth',
  'cliente',
  'gerente',
  'conta',
  'saga',
  'email',
  'localhost',
  '127.0.0.1',
]);

const CONTA_WRITE_RELS = ['deposito', 'saque', 'transferencia', 'extrato'] as const;

export type HateoasUser = {
  cpf: string;
  tipo: string;
};

export type HateoasContext = {
  publicUrl: string;
  user?: HateoasUser;
  requestUrl?: string;
};

type LinkMap = Record<string, { href: string }>;

export function rewriteHref(href: string, publicUrl: string): string {
  try {
    const current = new URL(href);
    if (!INTERNAL_HOSTS.has(current.hostname)) {
      return href;
    }
    const pub = new URL(publicUrl);
    current.protocol = pub.protocol;
    current.host = pub.host;
    return current.toString();
  } catch {
    return href;
  }
}

export function rewriteLinks(value: unknown, publicUrl: string): unknown {
  if (Array.isArray(value)) {
    return value.map((item) => rewriteLinks(item, publicUrl));
  }
  if (value && typeof value === 'object') {
    const entries = Object.entries(value as Record<string, unknown>).map(([key, nested]) => {
      if (key === 'href' && typeof nested === 'string') {
        return [key, rewriteHref(nested, publicUrl)];
      }
      return [key, rewriteLinks(nested, publicUrl)];
    });
    return Object.fromEntries(entries);
  }
  return value;
}

export function rewriteLocation(
  location: string | undefined,
  publicUrl: string,
): string | undefined {
  if (!location) {
    return undefined;
  }
  if (location.startsWith('/')) {
    return location;
  }
  return rewriteHref(location, publicUrl);
}

export function applyHateoas(value: unknown, ctx: HateoasContext): unknown {
  const rewritten = rewriteLinks(value, ctx.publicUrl);
  const withList = applyListSelf(rewritten, ctx);
  return applyConditionalLinks(withList, ctx.user);
}

export function cacheableCadastro(value: unknown, publicUrl: string): unknown {
  const rewritten = rewriteLinks(value, publicUrl);
  return ensureCadastroLinks(rewritten, publicUrl);
}

function applyListSelf(value: unknown, ctx: HateoasContext): unknown {
  const path = pathnameOf(ctx.requestUrl);
  if (!ctx.requestUrl || !ctx.publicUrl || !isRecord(value)) {
    return value;
  }
  const href = `${ctx.publicUrl}${ctx.requestUrl}`;
  if (path === '/clientes' && Array.isArray(value.clientes)) {
    value._links = { ...linkMap(value._links), self: { href } };
  }
  if (path === '/solicitacoes' && Array.isArray(value.solicitacoes)) {
    value._links = { ...linkMap(value._links), self: { href } };
  }
  if (path === '/gerentes' && Array.isArray(value.gerentes)) {
    value._links = {
      ...linkMap(value._links),
      self: { href },
      criacao: { href: `${ctx.publicUrl}/gerentes` },
    };
  }
  return value;
}

export function applyConditionalLinks(value: unknown, user?: HateoasUser): unknown {
  if (Array.isArray(value)) {
    return value.map((item) => applyConditionalLinks(item, user));
  }
  if (!isRecord(value)) {
    return value;
  }
  const next: Record<string, unknown> = {};
  for (const [key, nested] of Object.entries(value)) {
    next[key] = key === '_links' ? nested : applyConditionalLinks(nested, user);
  }
  if (isRecord(next._links)) {
    next._links = shapeLinks(next, linkMap(next._links), user);
  }
  return next;
}

function shapeLinks(
  resource: Record<string, unknown>,
  links: LinkMap,
  user?: HateoasUser,
): LinkMap {
  const shaped = { ...links };
  if (isGerenteCadastro(resource)) {
    const href =
      shaped.self?.href ??
      (typeof resource.cpf === 'string' ? `/gerentes/${resource.cpf}` : undefined);
    if (resource.ativo === true && href) {
      shaped.self = shaped.self ?? { href };
      shaped.atualizacao = { href };
      if (user?.cpf !== resource.cpf) {
        shaped.remocao = { href };
      } else {
        delete shaped.remocao;
      }
    } else {
      delete shaped.atualizacao;
      delete shaped.remocao;
    }
  }
  if (isContaResource(resource) && user?.tipo === 'GERENTE') {
    for (const rel of CONTA_WRITE_RELS) {
      delete shaped[rel];
    }
  }
  return shaped;
}

function ensureCadastroLinks(value: unknown, publicUrl: string): unknown {
  if (!isRecord(value)) {
    return value;
  }
  const cpf = typeof value.cpf === 'string' ? value.cpf : '';
  if (!cpf) {
    return value;
  }
  const links = linkMap(value._links);
  if (isClienteCadastro(value)) {
    value._links = {
      ...links,
      self: { href: `${publicUrl}/clientes/${cpf}` },
      conta: { href: `${publicUrl}/clientes/${cpf}/conta` },
    };
  } else if (isGerenteCadastro(value)) {
    const href = `${publicUrl}/gerentes/${cpf}`;
    const next: LinkMap = { ...links, self: { href } };
    if (value.ativo === true) {
      next.atualizacao = { href };
      next.remocao = { href };
    } else {
      delete next.atualizacao;
      delete next.remocao;
    }
    value._links = next;
  }
  return value;
}

function isClienteCadastro(value: Record<string, unknown>): boolean {
  return isRecord(value.endereco) && value.salario !== undefined;
}

function isGerenteCadastro(value: Record<string, unknown>): boolean {
  return (
    typeof value.ativo === 'boolean' && value.telefone !== undefined && !isRecord(value.endereco)
  );
}

function isContaResource(value: Record<string, unknown>): boolean {
  return (
    typeof value.numero === 'string' &&
    typeof value.cpfCliente === 'string' &&
    value.saldo !== undefined &&
    value.dataCriacao !== undefined
  );
}

function linkMap(value: unknown): LinkMap {
  if (!isRecord(value)) {
    return {};
  }
  const links: LinkMap = {};
  for (const [rel, item] of Object.entries(value)) {
    const href = isRecord(item) && typeof item.href === 'string' ? item.href : undefined;
    if (href) {
      links[rel] = { href };
    }
  }
  return links;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return Boolean(value) && typeof value === 'object' && !Array.isArray(value);
}

function pathnameOf(url: string | undefined): string {
  if (!url) {
    return '';
  }
  const q = url.indexOf('?');
  return q === -1 ? url : url.slice(0, q);
}
