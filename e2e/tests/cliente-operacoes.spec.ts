import { test, expect } from '@playwright/test';
import { aceitarConfirmacao, irOperacaoCliente, login } from './helpers';
import { SEED } from './fixtures/data';

test.describe.configure({ mode: 'serial' });

test.describe('R3–R7 — Operações do cliente (UI)', () => {
  test('R3 happy: depósito', async ({ page }) => {
    await login(page, SEED.cliente4.email, SEED.cliente4.senha);
    await irOperacaoCliente(page, 'Depósito');
    await expect(page).toHaveURL(/deposito/);
    await page.fill('#valor', '100');
    await page.getByRole('button', { name: 'Depositar' }).click();
    await aceitarConfirmacao(page, 'Depositar');
    await expect(
      page.getByRole('status').or(page.getByRole('alert')).filter({ hasText: /sucesso|depositado|Atualizando/i }),
    ).toBeVisible({ timeout: 30_000 });
  });

  test('R4 happy: saque', async ({ page }) => {
    await login(page, SEED.cliente4.email, SEED.cliente4.senha);
    await irOperacaoCliente(page, 'Saque');
    await page.fill('#valorSaque', '50');
    await page.getByRole('button', { name: 'Sacar' }).click();
    await aceitarConfirmacao(page, 'Sacar');
    await expect(
      page.locator('[role="status"]').filter({ hasText: /Saque|sucesso|Atualizando/i }),
    ).toBeVisible({ timeout: 30_000 });
  });

  test('R4 sad: saque acima do saldo', async ({ page }) => {
    await login(page, SEED.cliente4.email, SEED.cliente4.senha);
    await irOperacaoCliente(page, 'Saque');
    await page.fill('#valorSaque', '999999999');
    await page.getByRole('button', { name: 'Sacar' }).click();
    await aceitarConfirmacao(page, 'Sacar');
    await expect(page.getByRole('alert').filter({ hasText: /saque|saldo|Não foi possível/i })).toBeVisible({
      timeout: 20_000,
    });
  });

  test('R5 happy: transferência para outra conta', async ({ page }) => {
    await login(page, SEED.cliente4.email, SEED.cliente4.senha);
    await irOperacaoCliente(page, 'Transferência');
    await page.fill('#contaDestino', SEED.cliente2.conta);
    await page.fill('#valorTransferencia', '10');
    await page.getByRole('button', { name: 'Transferir' }).click();
    await aceitarConfirmacao(page, 'Transferir');
    await expect(
      page.locator('[role="status"]').filter({ hasText: /Transferência|sucesso|Atualizando/i }),
    ).toBeVisible({ timeout: 30_000 });
  });

  test('R5 sad: transferência para a própria conta', async ({ page }) => {
    await login(page, SEED.cliente.email, SEED.cliente.senha);
    const contaOrigem = SEED.cliente.conta;
    await irOperacaoCliente(page, 'Transferência');
    await page.fill('#contaDestino', contaOrigem);
    await page.fill('#valorTransferencia', '10');
    await expect(page.getByText(/não pode ser a mesma/i)).toBeVisible();
    await expect(page.getByRole('button', { name: 'Transferir' })).toBeDisabled();
  });

  test('R6 happy: extrato no período', async ({ page }) => {
    await login(page, SEED.cliente.email, SEED.cliente.senha);
    await irOperacaoCliente(page, 'Extrato');
    await page.fill('#inicio', '2020-01-01');
    await page.fill('#fim', '2020-12-31');
    await page.getByRole('button', { name: 'Consultar' }).click();
    await expect(page.locator('table.extrato tbody tr').first()).toBeVisible({ timeout: 25_000 });
  });
});
