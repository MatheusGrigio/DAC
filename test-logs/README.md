# Logs de execução de testes

Saídas capturadas (`Tee-Object`, campanhas manuais, depuração Playwright). **Não são código** — podem ser apagadas a qualquer momento.

| Padrão no nome | Camada |
|----------------|--------|
| `test-bantads-*` | `test_bantads-main` (pytest DS152) |
| `contract-tests-*` | `backend/contract-tests` |
| `gateway-*` | `backend/gateway` (`npm test`) |
| `gradle-*` | Gradle MSs |
| `frontend-*` | Angular unit/build |
| `playwright-*` | E2E Playwright (`e2e/`) |
| `httpie-e2e-*` | `httpie/run-transacoes-e2e.py` |
| `campanha-*.txt` | Resumo de `run-test-campanha.ps1` |

Novas campanhas: `.\run-test-campanha.ps1` grava aqui com timestamp.