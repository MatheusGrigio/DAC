import { expect, type Page } from '@playwright/test';
import type { AutocadastroPayload } from './fixtures/data';
import { gatewayReboot } from './fixtures/gateway';

export { gatewayReboot };

export async function login(page: Page, email: string, senha: string): Promise<void> {
  await page.goto('/login');
  await page.fill('#email', email);
  await page.fill('#senha', senha);
  await page.getByRole('button', { name: /entrar/i }).click();
  await page.waitForURL(/\/(cliente|gerente)\//, { timeout: 20_000 });
}

/** Login gerente aguardando o GET de solicitações do dashboard. */
export async function loginGerenteDashboard(page: Page, email: string, senha: string): Promise<void> {
  await page.goto('/login');
  await page.fill('#email', email);
  await page.fill('#senha', senha);
  const lista = page.waitForResponse(
    (r) => r.url().includes('/solicitacoes') && r.request().method() === 'GET',
    { timeout: 90_000 },
  );
  await page.getByRole('button', { name: /entrar/i }).click();
  await page.waitForURL(/gerente\/dashboard/, { timeout: 20_000 });
  const resposta = await lista;
  expect(resposta.status()).toBe(200);
}

export async function loginExpectFailure(page: Page, email: string, senha: string): Promise<void> {
  await page.goto('/login');
  await page.fill('#email', email);
  await page.fill('#senha', senha);
  const responseWait = page.waitForResponse(
    (r) => r.url().includes('/login') && r.request().method() === 'POST',
    { timeout: 45_000 },
  );
  await page.getByRole('button', { name: /^entrar$/i }).click();
  const response = await responseWait;
  expect(response.status()).toBe(401);
  const body = (await response.json()) as { auth?: boolean; message?: string };
  expect(body.auth).toBe(false);
  await expect(page).toHaveURL(/\/login/);
}

export async function preencherAutocadastro(page: Page, dados: AutocadastroPayload): Promise<void> {
  await page.goto('/autocadastro');
  await page.fill('#nome', dados.nome);
  await page.fill('#cpf', dados.cpf);
  await page.fill('#email', dados.email);
  await page.fill('#telefone', dados.telefone);
  await page.fill('#salario', dados.salario);
  await page.fill('#cep', dados.cep);
  await page.fill('#logradouro', dados.logradouro);
  await page.fill('#numero', dados.numero);
  if (dados.complemento) await page.fill('#complemento', dados.complemento);
  await page.fill('#cidade', dados.cidade);
  await page.fill('#uf', dados.uf);
}

export async function enviarAutocadastro(page: Page, dados: AutocadastroPayload): Promise<void> {
  await preencherAutocadastro(page, dados);
  await page.getByRole('button', { name: /solicitar cadastro/i }).click();
}

export async function irOperacaoCliente(page: Page, titulo: string): Promise<void> {
  const card = page.locator('.card').filter({ hasText: titulo });
  await card.getByRole('link', { name: 'Acessar' }).click();
}

export async function aceitarConfirmacao(page: Page): Promise<void> {
  page.once('dialog', (d) => d.accept());
}

export async function rejeitarConfirmacao(page: Page): Promise<void> {
  page.once('dialog', (d) => d.dismiss());
}

export async function promptComMotivo(page: Page, motivo: string): Promise<void> {
  page.once('dialog', (d) => d.accept(motivo));
}

export async function formatarCpfTela(cpf: string): string {
  const d = cpf.replace(/\D/g, '');
  return `${d.slice(0, 3)}.${d.slice(3, 6)}.${d.slice(6, 9)}-${d.slice(9, 11)}`;
}

export async function rebootEntreBlocos(): Promise<void> {
  await gatewayReboot();
}

export async function aguardarSolicitacoesGerente(page: Page): Promise<void> {
  await expect(page.locator('section.solicitacoes-section .carregando')).toBeHidden({
    timeout: 60_000,
  });
  await expect(page.locator('section.solicitacoes-section .tabela')).toBeVisible();
}