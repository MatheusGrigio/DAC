import { test, expect } from '@playwright/test';
import {
  aceitarConfirmacao,
  aguardarSolicitacoesGerente,
  formatarCpfTela,
  login,
  loginGerenteDashboard,
  promptComMotivo,
} from './helpers';
import { dadosAutocadastro, SEED } from './fixtures/data';
import { criarSolicitacaoApi } from './fixtures/gateway-api';
import { pollSenhaDoEmail } from './fixtures/outbox';
import { gatewayReboot } from './fixtures/gateway';

test.describe.configure({ mode: 'serial' });

test.beforeAll(async () => {
  await gatewayReboot();
});

test.describe('R9–R10 — Solicitações (UI)', () => {
  test('R9 happy: aprovar autocadastro e login com senha do outbox', async ({ page }) => {
    const solicitação = dadosAutocadastro();
    await criarSolicitacaoApi(solicitação);

    await loginGerenteDashboard(page, SEED.gerente.email, SEED.gerente.senha);
    await aguardarSolicitacoesGerente(page);
    const cpfFormatado = await formatarCpfTela(solicitação.cpf);
    const linha = page.locator('section.solicitacoes-section tr').filter({ hasText: cpfFormatado });
    await expect(linha).toBeVisible({ timeout: 30_000 });

    await aceitarConfirmacao(page);
    await linha.getByRole('button', { name: 'Aprovar' }).click();
    await expect(page.getByText(/aprovado com sucesso/i)).toBeVisible({ timeout: 90_000 });

    const senha = await pollSenhaDoEmail(solicitação.email);
    await page.getByRole('button', { name: /sair/i }).click();
    await login(page, solicitação.email, senha);
    await expect(page).toHaveURL(/cliente\/dashboard/);
    await expect(page.getByText('Saldo atual:')).toBeVisible({ timeout: 20_000 });
  });

  test('R10 happy: rejeitar solicitação com motivo', async ({ page }) => {
    const solicitação = dadosAutocadastro();
    await criarSolicitacaoApi(solicitação);

    await loginGerenteDashboard(page, SEED.gerente.email, SEED.gerente.senha);
    await aguardarSolicitacoesGerente(page);
    const cpfFormatado = await formatarCpfTela(solicitação.cpf);
    const linha = page.locator('section.solicitacoes-section tr').filter({ hasText: cpfFormatado });
    await expect(linha).toBeVisible({ timeout: 30_000 });

    await promptComMotivo(page, 'Documentação incompleta E2E');
    await linha.getByRole('button', { name: 'Recusar' }).click();
    await expect(page.getByText(/rejeitado com sucesso/i)).toBeVisible({ timeout: 20_000 });
    await expect(linha.getByText(/Não aprovado/i)).toBeVisible({ timeout: 15_000 });
  });

  test('R10 sad: cancelar prompt de rejeição não altera status', async ({ page }) => {
    const solicitação = dadosAutocadastro();
    await criarSolicitacaoApi(solicitação);

    await loginGerenteDashboard(page, SEED.gerente.email, SEED.gerente.senha);
    await aguardarSolicitacoesGerente(page);
    const cpfFormatado = await formatarCpfTela(solicitação.cpf);
    const linha = page.locator('section.solicitacoes-section tr').filter({ hasText: cpfFormatado });
    await expect(linha).toBeVisible({ timeout: 30_000 });

    page.once('dialog', (d) => d.dismiss());
    await linha.getByRole('button', { name: 'Recusar' }).click();
    await expect(linha.getByText(/Pendente/i)).toBeVisible();
  });
});