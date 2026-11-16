import { test, expect } from '@playwright/test';
import { login, loginExpectFailure } from './helpers';
import { SEED } from './fixtures/data';

test.describe('R2 — Autenticação (UI)', () => {
  test('R2A happy: login cliente', async ({ page }) => {
    await login(page, SEED.cliente.email, SEED.cliente.senha);
    await expect(page).toHaveURL(/cliente\/dashboard/);
    await expect(page.getByRole('heading', { name: 'BANTADS - Área do Cliente' })).toBeVisible();
  });

  test('R2A happy: login gerente', async ({ page }) => {
    await login(page, SEED.gerente.email, SEED.gerente.senha);
    await expect(page).toHaveURL(/gerente\/(solicitacoes|dashboard)/);
    await expect(page.getByRole('heading', { name: 'BANTADS - Área do Gerente' })).toBeVisible();
  });

  test('R2A sad: credenciais inválidas', async ({ page }) => {
    await loginExpectFailure(page, SEED.cliente.email, 'senha-errada');
  });

  test('R2A sad: validação de e-mail no formulário', async ({ page }) => {
    await page.goto('/login');
    await page.fill('#email', 'nao-e-email');
    await page.locator('#email').blur();
    await expect(page.getByText(/Informe um e-mail válido/i)).toBeVisible();
  });

  test('R2B happy: logout cliente', async ({ page }) => {
    await login(page, SEED.cliente.email, SEED.cliente.senha);
    await page.getByRole('button', { name: /sair/i }).click();
    await expect(page).toHaveURL(/login/);
  });

  test('R2B happy: logout gerente', async ({ page }) => {
    await login(page, SEED.gerente.email, SEED.gerente.senha);
    await page.getByRole('button', { name: /sair/i }).click();
    await expect(page).toHaveURL(/login/);
  });
});