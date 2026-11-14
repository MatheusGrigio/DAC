# RELATÓRIO DE TESTES COMPLETO — BANTADS

| Campo | Valor |
|-------|--------|
| **Data/hora** | 2026-11-14 20:26 (BRT) — `docker compose down -v` + frota nova |
| **Git `HEAD`** | `eebb11dbadc7eeaf4a2d4a154e0d2b79bd5d26b0` |
| **Docker frota** | OK (`down -v` → `up -d --build`; init Postgres exige LF em `02-users.sh`) |
| **POST `/reboot`** | Antes de `test_bantads`, `contract-tests` e em cada `global-setup` Playwright |
| **`docs/`** | Ausente — oráculo: `json-catalog/`, `relatorio de integração.md`, suítes oficiais |

---

## Resumo por camada (execução final)

| Camada | Total | Passed | Failed | Skipped | Exit | Duração |
|--------|------:|-------:|-------:|--------:|-----:|--------:|
| **test_bantads-main** | 81 | 68 | 0 | 13 | 0 | ~17s |
| **backend/contract-tests** | 73 | 73 | 0 | 0 | 0 | ~37s |
| **gateway `npm test`** | 56 | 56 | 0 | 0 | 0 | ~9s |
| **Gradle `test` (frota)** | — | OK | 0 | — | 0 | ~21s |
| **frontend `npm test`** | 17 | 17 | 0 | 0 | 0 | ~7s |
| **frontend `npm run build`** | — | OK | 0 | warn budget | 0 | ~8s |
| **Playwright `e2e/`** | 27 | 27 | 0 | 0 | 0 | ~40s |
| **httpie E2E** | — | — | — | SKIPPED | — | não executado |

### Skips esperados (`test_bantads`)

13 testes: login interativo de cliente novo (senha por e-mail) e `test_10_*` dependentes de `token_cli_novo_*` — normais em CI sem `automatic_password` / terminal.

---

## Correções de produto (sessão anterior + retomada)

| Área | Alteração |
|------|-----------|
| `gradlew` | LF para build Docker no Alpine |
| Gateway `session.ts` | Login não invalida JWT anteriores (DS152 `R02_logout_5`) |
| Gateway ACL + MS Conta | Gerente pode consultar `GET .../extrato` |
| **Playwright `e2e/tests/*`** | Seletores alinhados à UI (H1, `#busca`, `page.goto`, validação autocadastro) |

---

## Testes Playwright (matriz UI R1–R16)

| Spec | Happy / sad (UI) |
|------|------------------|
| `auth-r2.spec.ts` | Login cliente/gerente, credenciais inválidas (401), validação e-mail, logout |
| `autocadastro-r1.spec.ts` | Validações, envio com dados únicos |
| `cliente-operacoes.spec.ts` | Depósito, saque (+ saldo insuficiente), transferência (+ mesma conta), extrato, dados cadastrais |
| `gerente-lista-relatorio.spec.ts` | Busca clientes (hit/miss), relatório 202 |
| `gerente-crud.spec.ts` | Listar, inserir (SAGA), editar, remover outro gerente, sem remover a si |
| `gerente-solicitacoes.spec.ts` | Aprovar (SAGA + login com senha do outbox), rejeitar, cancelar prompt |

Fixtures: `e2e/tests/fixtures/data.ts`, `outbox.ts` (senha R9 em `outbox/`), `gateway-api.ts` (setup mockado).

`e2e/global-setup.ts`: health + `/reboot`. Relatório: `e2e/playwright-report/`.

**Produto:** `app-button` com `RouterLink`; dashboard gerente em **signals** + `queueMicrotask` no load; botão **Atualizar lista**; login sincroniza token/signals no `AuthService`.

---

## Frontend unitário

`TestBed` nos specs de `Login`, `Deposito`, `Saque` e `Autocadastro`: `provideRouter([])` + `provideHttpClient()` / `provideHttpClientTesting()` (uso de `RouterLink` no `app-button` e serviços HTTP). **17/17** após correção.

Build Angular: sucesso (warning de budget ~599 kB).

---

## Histórico desta campanha

- **2026-11-14 rodada zero:** `down -v` falhou Postgres (exit 127) por CRLF em `db/postgres/init/02-users.sh`; corrigido para LF; suítes oficiais na ordem do relatório — Playwright **27/27** após reboot pré-E2E.
- 1ª rodada `test_bantads`: 22 falhas (sessão Redis) — corrigido.
- 2ª rodada: 67 passed, 1 timeout em relatório (falso negativo) — **3ª rodada: 68 passed, exit 0**.
- `contract-tests`: 73/73 após frota estável.
- Gradle: falhou com Docker 500; após restart — **BUILD SUCCESSFUL**.
- Playwright: **27/27** — matriz caixa-preta R1–R16 (UI); R8 extrato gerente só na API.

---

## Comandos para reproduzir

```powershell
cd C:\Users\zuria\CODE\UFPR\DAC\DAC
docker compose up -d --build
Invoke-RestMethod http://localhost:3000/reboot -Method POST -ContentType application/json -Body "{}"

cd test_bantads-main
Remove-Item cache.json -ErrorAction SilentlyContinue
python -m pytest -v

cd ..\backend\contract-tests
.\.venv\Scripts\python -m pytest -s -v

cd ..\gateway
npm test

cd ..\services
.\gradlew.bat --no-daemon --no-parallel --max-workers=1 test

cd ..\..\frontend
npm test -- --watch=false
npm run build

cd ..\e2e
npx playwright test
```

---

## Lacunas documentadas

- **R11 502/503 live:** mock em `backend/gateway/test/composition.test.ts`.
- **httpie** `run-transacoes-e2e.py`: não rodado nesta retomada.
- **Cliente novo E2E DS152:** skips por senha interativa — `config.json` → `automatic_password` se houver `senha_recente.txt`.
- **R8 extrato gerente:** sem tela dedicada no front (só cliente); API em contract/pytest.

---

## Referências

- [RELATORIO-BACKEND-TESTES-FLUXOS.md](./RELATORIO-BACKEND-TESTES-FLUXOS.md)
- Saídas (rodada zero): `test-bantads-output-rodada-zero.txt`, `contract-tests-output-rodada-zero.txt`, `gateway-test-output-rodada-zero.txt`, `gradle-test-output-rodada-zero.txt`, `frontend-test-output-rodada-zero.txt`, `frontend-build-output-rodada-zero.txt`, `playwright-run-output-rodada-zero.txt`