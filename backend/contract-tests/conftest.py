from __future__ import annotations

import json
import time
from datetime import datetime, timezone
from pathlib import Path

import pytest
import requests
from dotenv import load_dotenv

from helpers import HEADERS, URL

load_dotenv(Path(__file__).resolve().parent / ".env")
load_dotenv(Path(__file__).resolve().parent / ".env.example")

REPO_ROOT = Path(__file__).resolve().parent.parent.parent
REPORT_PATH = REPO_ROOT / "RELATORIO-BACKEND-TESTES-FLUXOS.md"
_RESULTS: list[dict] = []


def _is_catalog_module(item: pytest.Item) -> bool:
    return "test_z_catalog" in Path(str(item.fspath)).name


@pytest.fixture(scope="session", autouse=True)
def wait_for_gateway():
    deadline = time.time() + 90
    last_err = ""
    while time.time() < deadline:
        try:
            resp = requests.get(URL + "/health", headers=HEADERS, timeout=5)
            if resp.status_code == 200 and resp.json().get("status") == "UP":
                return
            last_err = resp.text
        except requests.RequestException as exc:
            last_err = str(exc)
        time.sleep(3)
    pytest.exit(f"Gateway indisponível em {URL} após 90s: {last_err}")


@pytest.fixture(scope="module", autouse=True)
def catalog_module_reboot(request):
    name = request.module.__name__
    if not name.startswith("test_z_catalog"):
        yield
        return
    resp = requests.post(URL + "/reboot", headers=HEADERS, timeout=90)
    assert resp.status_code == 200, resp.text
    yield


@pytest.hookimpl(tryfirst=True, hookwrapper=True)
def pytest_runtest_makereport(item, call):
    outcome = yield
    rep = outcome.get_result()
    if rep.when != "call":
        return
    _RESULTS.append(
        {
            "nodeid": item.nodeid,
            "outcome": rep.outcome,
            "duration": getattr(rep, "duration", 0),
            "catalog": _is_catalog_module(item),
            "longrepr": str(rep.longrepr) if rep.failed else "",
        }
    )


def pytest_sessionfinish(session, exitstatus):
    passed = sum(1 for r in _RESULTS if r["outcome"] == "passed")
    failed = sum(1 for r in _RESULTS if r["outcome"] == "failed")
    skipped = sum(1 for r in _RESULTS if r["outcome"] == "skipped")
    total = len(_RESULTS)
    now = datetime.now(timezone.utc).astimezone().strftime("%Y-%m-%d %H:%M:%S %z")

    lines = [
        "# Relatório — backend testes de fluxos (BANTADS)",
        "",
        f"Gerado em: **{now}**",
        f"Gateway: `{URL}`",
        f"Pytest exit code: **{exitstatus}**",
        "",
        "## Resumo",
        "",
        f"| Resultado | Quantidade |",
        f"|-----------|------------|",
        f"| Passou | {passed} |",
        f"| Falhou | {failed} |",
        f"| Ignorado | {skipped} |",
        f"| Total | {total} |",
        "",
        "## Suítes",
        "",
        "- **T00–T16** (`test_t*.py`): aceite stateful do enunciado (equivalente ao roteiro HTTPie principal).",
        "- **Catálogo** (`test_z_catalog_*.py`): requests/responses de `json-catalog/` e transações TX-* não cobertas só pelo T00–T16.",
        "- **Gateway** (`npm test` em `backend/gateway`): ver seção abaixo se o script `run-all-backend-tests.ps1` foi executado.",
        "",
        "## Detalhe por teste",
        "",
        "| Teste | Resultado | Duração (s) |",
        "|-------|-----------|-------------|",
    ]
    for row in _RESULTS:
        dur = f"{row['duration']:.2f}" if row["duration"] else "—"
        flag = "catálogo" if row["catalog"] else "aceite"
        lines.append(f"| `{row['nodeid']}` ({flag}) | {row['outcome']} | {dur} |")

    if failed:
        lines.extend(["", "## Falhas", ""])
        for row in _RESULTS:
            if row["outcome"] == "failed":
                lines.append(f"### `{row['nodeid']}`")
                lines.append("")
                lines.append("```")
                lines.append(row["longrepr"][:4000])
                lines.append("```")
                lines.append("")

    lines.extend(
        [
            "",
            "## Cobertura conhecida (não E2E na frota)",
            "",
            "- **TX-R11 502/503**: validado em `backend/gateway/test/composition.test.ts` (mock); E2E exige falha de MS ou atraso de projeção difícil de reproduzir de forma determinística.",
            "- **502/504 genérico** (MS fora): não simulado na suíte live.",
            "- Tutoriais **transacoes/00-*** (JWT interno, Rabbit wiring): cobertos indiretamente por SAGA + filas; não há assert em cada passo do diagrama.",
            "",
            "## Como repetir",
            "",
            "```powershell",
            "wsl -d Ubuntu -- bash -lc 'cd /mnt/e/CODE/DAC-1 && docker compose up -d'",
            "cd backend/contract-tests",
            "python -m pytest -s -v",
            "cd ../gateway && npm test",
            "```",
            "",
        ]
    )

    REPORT_PATH.write_text("\n".join(lines), encoding="utf-8")
    meta = REPO_ROOT / "backend" / "contract-tests" / ".last-run.json"
    meta.write_text(
        json.dumps(
            {
                "exitstatus": exitstatus,
                "passed": passed,
                "failed": failed,
                "skipped": skipped,
                "total": total,
                "report": str(REPORT_PATH),
            },
            indent=2,
        ),
        encoding="utf-8",
    )
