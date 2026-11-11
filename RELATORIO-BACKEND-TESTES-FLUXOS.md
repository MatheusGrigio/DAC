# Relatório — backend testes de fluxos (BANTADS)

Gerado em: **2026-10-27 23:34:09 -0300**
Gateway: `http://localhost:3000`
Pytest exit code: **0**

## Resumo

| Resultado | Quantidade |
|-----------|------------|
| Passou | 73 |
| Falhou | 0 |
| Ignorado | 0 |
| Total | 73 |

## Suítes

- **T00–T16** (`test_t*.py`): aceite stateful do enunciado (equivalente ao roteiro HTTPie principal).
- **Catálogo** (`test_z_catalog_*.py`): requests/responses de `json-catalog/` e transações TX-* não cobertas só pelo T00–T16.
- **Gateway** (`npm test` em `backend/gateway`): ver seção abaixo se o script `run-all-backend-tests.ps1` foi executado.

## Detalhe por teste

| Teste | Resultado | Duração (s) |
|-------|-----------|-------------|
| `test_t00_reboot.py::test_t00_reboot` (aceite) | passed | 1.00 |
| `test_t00_reboot.py::test_t00b_health` (aceite) | passed | 0.02 |
| `test_t00c_catalog_seed.py::test_t00c_catalog_json_seed` (aceite) | passed | 0.45 |
| `test_t01_solicitacoes.py::test_t01_autocadastro` (aceite) | passed | 0.03 |
| `test_t01_solicitacoes.py::test_t01d_duplicate_cpf_or_email` (aceite) | passed | 0.03 |
| `test_t02_auth.py::test_t02a_without_token` (aceite) | passed | 0.01 |
| `test_t02_auth.py::test_t02b_invalid_token` (aceite) | passed | 0.03 |
| `test_t02_auth.py::test_t02c_login_invalid` (aceite) | passed | 0.06 |
| `test_t02_auth.py::test_t02d_login_seed_cliente` (aceite) | passed | 0.10 |
| `test_t02_auth.py::test_t02e_login_seed_gerente` (aceite) | passed | 0.09 |
| `test_t02_auth.py::test_t02f_logout_then_reuse_token` (aceite) | passed | 0.10 |
| `test_t03_conta.py::test_t03_own_account` (aceite) | passed | 0.11 |
| `test_t03_conta.py::test_t03p_other_account_forbidden` (aceite) | passed | 0.11 |
| `test_t04-07_movimentacoes.py::test_t04_deposito` (aceite) | passed | 2.35 |
| `test_t04-07_movimentacoes.py::test_t05_saque` (aceite) | passed | 2.13 |
| `test_t04-07_movimentacoes.py::test_t06_transferencia` (aceite) | passed | 4.25 |
| `test_t04-07_movimentacoes.py::test_t07_extrato` (aceite) | passed | 0.10 |
| `test_t08_10_14_gerente.py::test_t08_lista_solicitacoes` (aceite) | passed | 0.09 |
| `test_t08_10_14_gerente.py::test_t10_rejeicao` (aceite) | passed | 0.12 |
| `test_t09_aprovacao.py::test_t09_aprovacao` (aceite) | passed | 0.76 |
| `test_t09_aprovacao.py::test_t09f_sem_prevalidacao` (aceite) | passed | 1.17 |
| `test_t09_aprovacao.py::test_t09f_email_de_gerente` (aceite) | passed | 0.68 |
| `test_t11_clientes.py::test_t11_busca_cat` (aceite) | passed | 0.10 |
| `test_t12_gerentes.py::test_t12_lista_gerentes` (aceite) | passed | 0.11 |
| `test_t13_gerentes.py::test_t13_inserir_gerente` (aceite) | passed | 2.73 |
| `test_t13_gerentes.py::test_t13_segundo_insert_e_email_duplicado` (aceite) | passed | 3.20 |
| `test_t14_atualiza_gerente.py::test_t14_atualiza_gerente` (aceite) | passed | 0.10 |
| `test_t15_remover_gerente.py::test_t15_nao_remove_a_si_mesmo` (aceite) | passed | 0.12 |
| `test_t15_remover_gerente.py::test_t15_remove_gadamantio_e_inativo_nao_loga` (aceite) | passed | 0.67 |
| `test_t15_remover_gerente.py::test_t15_inexistente_ou_ja_inativo_falha_no_job` (aceite) | passed | 0.65 |
| `test_t16_relatorio.py::test_t16_relatorio_clientes` (aceite) | passed | 0.13 |
| `test_t16_relatorio.py::test_t16_cliente_nao_acessa` (aceite) | passed | 0.07 |
| `test_t_hateoas_dinheiro.py::test_t_hash_hateoas_excecoes_sem_links` (aceite) | passed | 0.11 |
| `test_z_catalog_async_rabbit.py::test_tx_r9_a_saga_conclui_filas_ociosas` (catálogo) | passed | 1.90 |
| `test_z_catalog_async_rabbit.py::test_tx_r13_a_inserir_gerente_saga` (catálogo) | passed | 0.75 |
| `test_z_catalog_async_rabbit.py::test_tx_r13_b_falha_email_duplicado` (catálogo) | passed | 0.67 |
| `test_z_catalog_cliente_conta.py::test_tx_cad_01_gerente_200` (catálogo) | passed | 0.11 |
| `test_z_catalog_cliente_conta.py::test_tx_cad_01_cliente_proprio` (catálogo) | passed | 0.08 |
| `test_z_catalog_cliente_conta.py::test_tx_cad_01_403_outro_cpf` (catálogo) | passed | 0.08 |
| `test_z_catalog_cliente_conta.py::test_tx_cad_01_404_nao_cliente` (catálogo) | passed | 0.11 |
| `test_z_catalog_cliente_conta.py::test_tx_r3a_conta_cpf` (catálogo) | passed | 0.12 |
| `test_z_catalog_cliente_conta.py::test_tx_r3b_conta_numero_cliente` (catálogo) | passed | 0.10 |
| `test_z_catalog_cliente_conta.py::test_tx_r3b_403_conta_outro_cliente` (catálogo) | passed | 0.09 |
| `test_z_catalog_cliente_conta.py::test_tx_r3b_gerente_sem_rels_operacao` (catálogo) | passed | 0.20 |
| `test_z_catalog_cliente_conta.py::test_tx_r4_400_valor_number` (catálogo) | passed | 0.08 |
| `test_z_catalog_cliente_conta.py::test_tx_r4_403_gerente_nao_deposita` (catálogo) | passed | 0.08 |
| `test_z_catalog_cliente_conta.py::test_tx_r5_422_saldo_insuficiente` (catálogo) | passed | 0.09 |
| `test_z_catalog_cliente_conta.py::test_tx_r6_transferencia_sem_token` (catálogo) | passed | 0.01 |
| `test_z_catalog_cliente_conta.py::test_tx_r7_extrato_query` (catálogo) | passed | 0.09 |
| `test_z_catalog_gerente.py::test_tx_r11_lista_sem_filtro_cinco_clientes` (catálogo) | passed | 0.08 |
| `test_z_catalog_gerente.py::test_tx_r12_lista_quatro_gerentes` (catálogo) | passed | 0.11 |
| `test_z_catalog_gerente.py::test_tx_cad_02_gerente_200` (catálogo) | passed | 0.10 |
| `test_z_catalog_gerente.py::test_tx_cad_02_404` (catálogo) | passed | 0.09 |
| `test_z_catalog_gerente.py::test_tx_r14_404_inexistente` (catálogo) | passed | 0.19 |
| `test_z_catalog_gerente.py::test_tx_r15_um_gerente_ativo_restante_self_delete_403` (catálogo) | passed | 1.68 |
| `test_z_catalog_infra_jobs.py::test_tx_infra_01_health` (catálogo) | passed | 0.03 |
| `test_z_catalog_infra_jobs.py::test_tx_infra_02_reboot_body` (catálogo) | passed | 0.49 |
| `test_z_catalog_infra_jobs.py::test_tx_job_01_404_inexistente` (catálogo) | passed | 0.09 |
| `test_z_catalog_infra_jobs.py::test_tx_job_01_403_outro_usuario` (catálogo) | passed | 0.19 |
| `test_z_catalog_infra_jobs.py::test_tx_job_02_409_result_nao_inline` (catálogo) | passed | 0.12 |
| `test_z_catalog_infra_jobs.py::test_tx_r2b_logout_204` (catálogo) | passed | 0.07 |
| `test_z_catalog_solicitacoes.py::test_tx_r1_201_pendente_links` (catálogo) | passed | 0.04 |
| `test_z_catalog_solicitacoes.py::test_tx_r1_409_cpf_solicitacao` (catálogo) | passed | 0.01 |
| `test_z_catalog_solicitacoes.py::test_tx_r1_409_email_solicitacao` (catálogo) | passed | 0.02 |
| `test_z_catalog_solicitacoes.py::test_tx_r1_409_cpf_ja_cliente` (catálogo) | passed | 0.02 |
| `test_z_catalog_solicitacoes.py::test_tx_r1_400_malformado` (catálogo) | passed | 0.01 |
| `test_z_catalog_solicitacoes.py::test_tx_r8b_a_200_pendente` (catálogo) | passed | 0.19 |
| `test_z_catalog_solicitacoes.py::test_tx_r8b_404_inexistente` (catálogo) | passed | 0.11 |
| `test_z_catalog_solicitacoes.py::test_tx_r8b_403_cliente` (catálogo) | passed | 0.12 |
| `test_z_catalog_solicitacoes.py::test_tx_r8a_lista_self_link` (catálogo) | passed | 0.10 |
| `test_z_catalog_solicitacoes.py::test_tx_r10_b_400_motivo_vazio` (catálogo) | passed | 0.09 |
| `test_z_catalog_solicitacoes.py::test_tx_r10_c_200_nao_aprovada_shape` (catálogo) | passed | 0.12 |
| `test_z_catalog_solicitacoes.py::test_tx_r8b_d_200_nao_aprovada` (catálogo) | passed | 0.09 |

## Cobertura conhecida (não E2E na frota)

- **TX-R11 502/503**: validado em `backend/gateway/test/composition.test.ts` (mock); E2E exige falha de MS ou atraso de projeção difícil de reproduzir de forma determinística.
- **502/504 genérico** (MS fora): não simulado na suíte live.
- Tutoriais **transacoes/00-*** (JWT interno, Rabbit wiring): cobertos indiretamente por SAGA + filas; não há assert em cada passo do diagrama.

## Como repetir

```powershell
wsl -d Ubuntu -- bash -lc 'cd /mnt/e/CODE/DAC-1 && docker compose up -d'
cd backend/contract-tests
python -m pytest -s -v
cd ../gateway && npm test
```
