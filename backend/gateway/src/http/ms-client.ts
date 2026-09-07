import { fetch } from 'undici';
import { MS_TIMEOUT_MS } from '../config.js';

export type MsResponse = {
  status: number;
  body: unknown;
  location?: string;
};

export type MsFetch = (
  url: string,
  init: { method: string; headers: Record<string, string>; body?: string; timeoutMs?: number },
) => Promise<{ status: number; headers: Headers; text: () => Promise<string> }>;

export async function msRequest(opts: {
  baseUrl: string;
  method: string;
  path: string;
  headers?: Record<string, string>;
  body?: unknown;
  timeoutMs?: number;
  fetchImpl?: MsFetch;
}): Promise<MsResponse> {
  const url = `${opts.baseUrl}${opts.path.startsWith('/') ? opts.path : `/${opts.path}`}`;
  const headers: Record<string, string> = { accept: 'application/json', ...(opts.headers ?? {}) };
  let body: string | undefined;
  if (opts.body !== undefined && opts.method !== 'GET' && opts.method !== 'HEAD') {
    headers['content-type'] = headers['content-type'] ?? 'application/json';
    body = typeof opts.body === 'string' ? opts.body : JSON.stringify(opts.body);
  }
  const fetchImpl = opts.fetchImpl ?? defaultFetch;
  try {
    const response = await fetchImpl(url, {
      method: opts.method,
      headers,
      body,
      timeoutMs: opts.timeoutMs,
    });
    const text = await response.text();
    let parsed: unknown = text.length === 0 ? null : text;
    if (text.length > 0) {
      try {
        parsed = JSON.parse(text) as unknown;
      } catch {
        parsed = text;
      }
    }
    return {
      status: response.status,
      body: parsed,
      location: response.headers.get('location') ?? undefined,
    };
  } catch (error) {
    const aborted =
      error instanceof Error &&
      (error.name === 'AbortError' || error.message.toLowerCase().includes('timeout'));
    return {
      status: aborted ? 504 : 502,
      body: {
        status: aborted ? 504 : 502,
        erro: aborted ? 'Gateway Timeout' : 'Bad Gateway',
        mensagem: aborted
          ? 'Tempo esgotado ao chamar o microsserviço'
          : 'Falha ao chamar o microsserviço',
      },
    };
  }
}

const defaultFetch: MsFetch = async (url, init) => {
  const response = await fetch(url, {
    method: init.method,
    headers: init.headers,
    body: init.body,
    signal: AbortSignal.timeout(init.timeoutMs ?? MS_TIMEOUT_MS),
  });
  return {
    status: response.status,
    headers: response.headers as unknown as Headers,
    text: () => response.text(),
  };
};
