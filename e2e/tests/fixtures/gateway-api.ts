import type { AutocadastroPayload } from './data';

const gateway = process.env.GATEWAY_URL ?? 'http://localhost:3000';

function salarioApi(valor: string): string {
  const n = Number.parseFloat(valor.replace(',', '.'));
  return Number.isFinite(n) ? n.toFixed(2) : '0.00';
}

/** Cria solicitação mockada via Gateway (setup de dados; ações gerente seguem na UI). */
export type SolicitacaoApi = Record<string, unknown>;

export async function criarSolicitacaoApi(dados: AutocadastroPayload): Promise<SolicitacaoApi> {
  const body = {
    nome: dados.nome.trim(),
    cpf: dados.cpf,
    email: dados.email.trim(),
    telefone: dados.telefone,
    salario: salarioApi(dados.salario),
    endereco: {
      cep: dados.cep,
      logradouro: dados.logradouro.trim(),
      numero: dados.numero.trim(),
      complemento: dados.complemento?.trim() || null,
      cidade: dados.cidade.trim(),
      uf: dados.uf.toUpperCase(),
    },
  };
  const r = await fetch(`${gateway}/solicitacoes`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
  if (!r.ok) {
    throw new Error(`POST /solicitacoes ${r.status}: ${await r.text()}`);
  }
  return (await r.json()) as SolicitacaoApi;
}

/** Garante que o GET da listagem na UI receba os itens mockados (setup; aprovação permanece na UI). */
export async function mockListagemSolicitacoesNaUi(
  page: import('@playwright/test').Page,
  itens: SolicitacaoApi[],
): Promise<void> {
  await page.route('**/solicitacoes**', async (route) => {
    const req = route.request();
    const url = req.url();
    if (
      req.method() === 'GET' &&
      !url.includes('/aprovacao') &&
      !url.includes('/rejeicao') &&
      !/\/solicitacoes\/[0-9]{11}(?:$|\?)/.test(url)
    ) {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ solicitacoes: itens }),
      });
      return;
    }
    await route.continue();
  });
}