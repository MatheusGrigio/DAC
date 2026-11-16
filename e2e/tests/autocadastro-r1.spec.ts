import { test, expect } from '@playwright/test';
import { enviarAutocadastro, preencherAutocadastro } from './helpers';
import { dadosAutocadastro } from './fixtures/data';

test.describe('R1 — Autocadastro (UI)', () => {
  test('R1 sad: validação ao tocar campos obrigatórios', async ({ page }) => {
    await page.goto('/autocadastro');
    await expect(page.getByRole('heading', { name: 'Solicitar autocadastro' })).toBeVisible();
    await page.locator('#nome').focus();
    await page.locator('#cpf').focus();
    await expect(page.getByText(/Informe o nome/i)).toBeVisible({ timeout: 10_000 });
    await page.locator('#cpf').focus();
    await page.locator('#email').focus();
    await expect(page.getByText(/Informe o CPF com 11 dígitos/i)).toBeVisible();
  });

  test('R1 happy: solicitação enviada com dados mockados', async ({ page }) => {
    const dados = dadosAutocadastro();
    await enviarAutocadastro(page, dados);
    await expect(page.getByText(/Solicitação enviada com sucesso/i)).toBeVisible({ timeout: 20_000 });
  });

  test('R1 sad: CPF com menos de 11 dígitos impede envio', async ({ page }) => {
    const dados = dadosAutocadastro({ cpf: '123' });
    await preencherAutocadastro(page, dados);
    await page.locator('#cpf').blur();
    await expect(page.getByText(/CPF com 11 dígitos/i)).toBeVisible();
    await expect(page.getByRole('button', { name: /solicitar cadastro/i })).toBeDisabled();
  });
});