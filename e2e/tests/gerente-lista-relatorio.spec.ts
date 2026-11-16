import { test, expect } from '@playwright/test';
import { login } from './helpers';
import { SEED } from './fixtures/data';

test.describe('R11 / R16 — Gerente lista e relatório (UI)', () => {
  test('R11 happy: navegação pelo dashboard e busca de clientes', async ({ page }) => {
    await login(page, SEED.gerente.email, SEED.gerente.senha);
    await page.goto('/gerente/clientes');
    await expect(page).toHaveURL(/gerente\/clientes/);
    await expect(page.getByRole('link', { name: 'Consultar clientes' })).toHaveClass(/ativo/);
    await page.fill('#busca', 'cat');
    await page.getByRole('button', { name: 'Buscar' }).click();
    await expect(page.getByText(/Catharyna/i)).toBeVisible({ timeout: 15_000 });
  });

  test('R11 sad: busca sem resultado', async ({ page }) => {
    await login(page, SEED.gerente.email, SEED.gerente.senha);
    await page.goto('/gerente/clientes');
    await page.fill('#busca', 'zzznenhumcliente999');
    await page.getByRole('button', { name: 'Buscar' }).click();
    await expect(page.getByText(/Catharyna/i)).toHaveCount(0, { timeout: 15_000 });
  });

  test('R16 happy: relatório assíncrono (202 + tabela)', async ({ page }) => {
    await login(page, SEED.gerente.email, SEED.gerente.senha);
    await page.goto('/gerente/relatorio');
    await expect(page).toHaveURL(/gerente\/relatorio/);
    await page.getByRole('button', { name: 'Atualizar relatório' }).click();
    await expect(page.locator('table tbody tr').first()).toBeVisible({ timeout: 90_000 });
  });
});