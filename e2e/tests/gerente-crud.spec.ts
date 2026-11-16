import { test, expect } from '@playwright/test';
import { aceitarConfirmacao, login } from './helpers';
import { dadosNovoGerente, SEED } from './fixtures/data';

test.describe.configure({ mode: 'serial' });

test.describe('R12–R15 — CRUD gerentes (UI)', () => {
  let cpfNovo = '';
  let nomeNovo = '';

  test('R12 happy: listar gerentes', async ({ page }) => {
    await login(page, SEED.gerente.email, SEED.gerente.senha);
    await page.goto('/gerente/gerentes');
    await expect(page).toHaveURL(/gerente\/gerentes/);
    await expect(page.getByRole('heading', { name: 'Gerentes' })).toBeVisible();
    await expect(page.getByText(SEED.gerente.email)).toBeVisible({ timeout: 15_000 });
  });

  test('R13 happy: inserir gerente (SAGA)', async ({ page }) => {
    const dados = dadosNovoGerente();
    cpfNovo = dados.cpf;
    nomeNovo = dados.nome;

    await login(page, SEED.gerente.email, SEED.gerente.senha);
    await page.goto('/gerente/gerentes');
    await page.getByRole('button', { name: 'Novo gerente' }).click();
    await page.fill('#nome-gerente', dados.nome);
    await page.fill('#cpf-gerente', dados.cpf);
    await page.fill('#email-gerente', dados.email);
    await page.fill('#telefone-gerente', dados.telefone);
    await page.fill('#senha-gerente', dados.senha);
    await page.getByRole('button', { name: 'Cadastrar gerente' }).click();
    await aceitarConfirmacao(page, 'Cadastrar');
    await expect(page.getByText(/cadastrado com sucesso/i)).toBeVisible({ timeout: 90_000 });
    await expect(page.locator('table tbody').getByText(dados.nome)).toBeVisible({ timeout: 15_000 });
  });

  test('R14 happy: atualizar nome e telefone', async ({ page }) => {
    test.skip(!cpfNovo, 'Depende do gerente criado em R13');

    await login(page, SEED.gerente.email, SEED.gerente.senha);
    await page.goto('/gerente/gerentes');
    const linha = page.locator('tr').filter({ hasText: nomeNovo });
    await linha.getByRole('button', { name: 'Editar' }).click();
    await page.fill('#edit-nome-gerente', `${nomeNovo} Atualizado`);
    await page.fill('#edit-telefone-gerente', '41988887777');
    await page.getByRole('button', { name: 'Salvar alterações' }).click();
    await expect(page.getByText(/atualizado com sucesso/i)).toBeVisible({ timeout: 20_000 });
  });

  test('R15 sad: gerente logado não vê Remover em si mesmo', async ({ page }) => {
    await login(page, SEED.gerente.email, SEED.gerente.senha);
    await page.goto('/gerente/gerentes');
    const selfRow = page.locator('tr').filter({ hasText: SEED.gerente.email });
    await expect(selfRow).toBeVisible();
    await expect(selfRow.getByRole('button', { name: 'Remover' })).toHaveCount(0);
  });

  test('R15 happy: remover gerente criado no teste', async ({ page }) => {
    test.skip(!cpfNovo, 'Depende do gerente criado em R13');

    await login(page, SEED.gerente.email, SEED.gerente.senha);
    await page.goto('/gerente/gerentes');
    const linha = page.locator('tr').filter({ hasText: nomeNovo });
    await linha.getByRole('button', { name: 'Remover' }).click();
    await aceitarConfirmacao(page, 'Remover');
    await expect(page.getByText(/removido com sucesso/i)).toBeVisible({ timeout: 90_000 });
  });
});
