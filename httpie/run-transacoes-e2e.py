#!/usr/bin/env python3
"""
E2E BANTADS via HTTPie CLI — alinhado a httpie/TX-*.md e json-catalog/TX-*.md.

Reutiliza catalog_assert e helpers de backend/contract-tests (mesma checagem JSON dos 73 pytest).

Uso:
  python httpie/run-transacoes-e2e.py
  python httpie/run-transacoes-e2e.py --skip-rabbit   # sem Management API :15672
  python httpie/run-transacoes-e2e.py --with-pytest   # encadeia pytest contract-tests

Pré-requisito: frota em http://localhost:3000, `http` no PATH, Python + requirements de contract-tests.
"""

from __future__ import annotations

import argparse
import subprocess
import sys
import time
from dataclasses import dataclass, field
from datetime import datetime, timezone
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent
CONTRACT = REPO / "backend" / "contract-tests"
sys.path.insert(0, str(CONTRACT))
sys.path.insert(0, str(Path(__file__).resolve().parent))

from catalog_assert import assert_catalog_json  # noqa: E402
from helpers import (  # noqa: E402
    CPF_CATHARYNA,
    CPF_CLEUDDONIO,
    CPF_GADAMANTIO,
    CPF_GERENTE,
    CPF_NOVO,
    CPF_T09,
    CPF_T09_DUP,
    EMAIL_T09,
    GERENTE_T13,
    GERENTE_T13_DUP,
    GERENTE_T13B,
    NOVA_SOLICITACAO,
    SOLICITACAO_T09,
    SOLICITACAO_T09_DUP,
    URL,
    senha_outbox,
)
from httpie_cli import HttpieCliError, http_request, require_httpie  # noqa: E402

try:
    from rabbitmq_helpers import (  # noqa: E402
        DLQ_QUEUES,
        SAGA_QUEUES,
        assert_dlq_clean,
        management_ready,
        wait_queues_idle,
    )
except ImportError:
    management_ready = lambda: False  # type: ignore
    wait_queues_idle = None  # type: ignore
    assert_dlq_clean = None  # type: ignore
    SAGA_QUEUES = []
    DLQ_QUEUES = []


REPORT_PATH = REPO / "RELATORIO-HTTPIE-E2E.md"


@dataclass
class Ctx:
    token_gerente: str = ""
    token_cliente: str = ""
    job_ids: list[str] = field(default_factory=list)
    log: list[str] = field(default_factory=list)

    def note(self, msg: str) -> None:
        self.log.append(msg)
        print(msg)


def _expect_status(ctx: Ctx, label: str, status: int, expected: int, body: object) -> None:
    if status != expected:
        raise AssertionError(f"{label}: HTTP {status} != {expected}: {body!r}")


def _catalog(ctx: Ctx, label: str, tx: str, heading: str, body: dict) -> None:
    assert_catalog_json(tx, heading, body)
    ctx.note(f"  OK catalog {tx} / {heading[:50]}…")


def wait_gateway(ctx: Ctx, timeout_s: float = 90) -> None:
    deadline = time.time() + timeout_s
    while time.time() < deadline:
        try:
            st, _, body = http_request("GET", f"{URL}/health")
            if st == 200 and isinstance(body, dict) and body.get("status") == "UP":
                ctx.note("Gateway UP")
                return
        except (HttpieCliError, OSError):
            pass
        time.sleep(3)
    raise SystemExit(f"Gateway indisponível em {URL}")


def poll_job_http(ctx: Ctx, job_id: str, token: str, timeout_s: float = 60) -> dict:
    deadline = time.time() + timeout_s
    last: dict = {}
    while time.time() < deadline:
        st, _, body = http_request("GET", f"{URL}/jobs/{job_id}/status", token=token)
        _expect_status(ctx, "job status", st, 200, body)
        last = body if isinstance(body, dict) else {}
        if last.get("status") in {"CONCLUIDO", "FALHA"}:
            return last
        time.sleep(0.5)
    raise AssertionError(f"job {job_id} não terminou: {last}")


def poll_saldo_http(ctx: Ctx, path: str, expected: str, token: str) -> None:
    time.sleep(2)
    for _ in range(4):
        st, _, body = http_request("GET", f"{URL}{path}", token=token)
        _expect_status(ctx, "saldo", st, 200, body)
        if isinstance(body, dict) and body.get("saldo") == expected:
            return
        time.sleep(5)
    raise AssertionError(f"saldo {expected} não projetado em {path}")


def login_http(ctx: Ctx, email: str, senha: str = "tads") -> str:
    st, _, body = http_request(
        "POST",
        f"{URL}/login",
        json_body={"email": email, "senha": senha},
    )
    _expect_status(ctx, "login", st, 200, body)
    assert isinstance(body, dict)
    return body["token"]


def run_flow(ctx: Ctx, *, check_rabbit: bool) -> None:
    require_httpie()

    # --- TX-INFRA ---
    ctx.note("== TX-INFRA-02 reboot ==")
    st, _, body = http_request("POST", f"{URL}/reboot")
    _expect_status(ctx, "reboot", st, 200, body)
    _catalog(ctx, "reboot", "TX-INFRA-02.md", "Response 200 — Seed recriado", body)

    ctx.note("== TX-INFRA-01 health ==")
    st, _, body = http_request("GET", f"{URL}/health")
    _expect_status(ctx, "health", st, 200, body)
    _catalog(ctx, "health", "TX-INFRA-01.md", "Response 200 — Gateway no ar", body)

    # --- TX-R2A ---
    ctx.note("== TX-R2A auth ==")
    st, _, body = http_request("GET", f"{URL}/clientes/{CPF_CATHARYNA}")
    _expect_status(ctx, "401 sem token", st, 401, body)
    _catalog(ctx, "401 ausente", "TX-R2A.md", "Token ausente em rota protegida", body)

    st, _, body = http_request(
        "POST",
        f"{URL}/login",
        json_body={"email": "cli1@bantads.com.br", "senha": "tads"},
    )
    _expect_status(ctx, "login cliente", st, 200, body)
    _catalog(ctx, "login cliente", "TX-R2A.md", "Autenticado (CLIENTE)", body)
    ctx.token_cliente = body["token"]

    st, _, body = http_request(
        "POST",
        f"{URL}/login",
        json_body={"email": "ger1@bantads.com.br", "senha": "tads"},
    )
    _expect_status(ctx, "login gerente", st, 200, body)
    _catalog(ctx, "login gerente", "TX-R2A.md", "Autenticado (GERENTE)", body)
    ctx.token_gerente = body["token"]

    st, _, body = http_request(
        "POST",
        f"{URL}/login",
        json_body={"email": "cli1@bantads.com.br", "senha": "errada"},
    )
    _expect_status(ctx, "login inválido", st, 401, body)
    _catalog(ctx, "login inválido", "TX-R2A.md", "Credenciais inválidas", body)

    # --- TX-CAD-01 / CAD-02 / R11 / R12 (seed) ---
    ctx.note("== TX-CAD-01 / CAD-02 / R11 / R12 seed ==")
    st, _, body = http_request("GET", f"{URL}/clientes/{CPF_CATHARYNA}", token=ctx.token_gerente)
    _expect_status(ctx, "CAD-01", st, 200, body)
    _catalog(ctx, "CAD-01", "TX-CAD-01.md", "Cliente (seed Catharyna)", body)

    st, _, body = http_request(
        "GET",
        f"{URL}/clientes",
        token=ctx.token_gerente,
        query={"busca": "Cat"},
    )
    _catalog(ctx, "R11 busca", "TX-R11.md", "Com filtro", body)

    st, _, body = http_request("GET", f"{URL}/gerentes/{CPF_GERENTE}", token=ctx.token_gerente)
    _catalog(ctx, "CAD-02", "TX-CAD-02.md", "Próprio gerente autenticado", body)

    st, _, body = http_request("GET", f"{URL}/gerentes", token=ctx.token_gerente)
    _catalog(ctx, "R12", "TX-R12.md", "Response 200 — Seed", body)

    # --- TX-R1 / R8 ---
    ctx.note("== TX-R1 autocadastro / R8 ==")
    st, _, body = http_request("POST", f"{URL}/solicitacoes", json_body=NOVA_SOLICITACAO)
    _expect_status(ctx, "R1 201", st, 201, body)
    _catalog(ctx, "R1 201", "TX-R1.md", "Solicitação registrada", body)

    st, _, body = http_request("POST", f"{URL}/solicitacoes", json_body=NOVA_SOLICITACAO)
    _expect_status(ctx, "R1 409 cpf", st, 409, body)
    _catalog(ctx, "R1 409", "TX-R1.md", "CPF já possui solicitação", body)

    st, _, body = http_request("GET", f"{URL}/solicitacoes/{CPF_NOVO}", token=ctx.token_gerente)
    _catalog(ctx, "R8B pendente", "TX-R8B.md", "Solicitação PENDENTE", body)

    st, _, body = http_request("GET", f"{URL}/solicitacoes", token=ctx.token_gerente)
    _catalog(ctx, "R8A lista", "TX-R8A.md", "Lista com item PENDENTE", body)

    # --- TX-R3 ---
    ctx.note("== TX-R3A / R3B conta ==")
    st, _, body = http_request(
        "GET",
        f"{URL}/clientes/{CPF_CATHARYNA}/conta",
        token=ctx.token_cliente,
    )
    _catalog(ctx, "R3A", "TX-R3A.md", "CLIENTE dono", body)

    st, _, body = http_request("GET", f"{URL}/contas/1291", token=ctx.token_cliente)
    _catalog(ctx, "R3B cliente", "TX-R3B.md", "CLIENTE dono", body)

    st, _, body = http_request(
        "GET",
        f"{URL}/clientes/{CPF_CLEUDDONIO}/conta",
        token=ctx.token_cliente,
    )
    _expect_status(ctx, "R3A 403", st, 403, body)
    _catalog(ctx, "R3A 403", "TX-R3A.md", "CLIENTE tentando conta de outro", body)

    # --- TX-R4–R7 movimentações ---
    ctx.note("== TX-R4–R7 movimentações ==")
    st, _, body = http_request(
        "POST",
        f"{URL}/contas/1291/deposito",
        token=ctx.token_cliente,
        json_body={"valor": "10.00"},
    )
    _expect_status(ctx, "R4 201", st, 201, body)
    _catalog(ctx, "R4", "TX-R4.md", "Depósito registrado", body)
    poll_saldo_http(ctx, f"/clientes/{CPF_CATHARYNA}/conta", "810.00", ctx.token_cliente)

    st, _, body = http_request(
        "POST",
        f"{URL}/contas/1291/saque",
        token=ctx.token_cliente,
        json_body={"valor": "900.00"},
    )
    _expect_status(ctx, "R5 422", st, 422, body)
    _catalog(ctx, "R5 422", "TX-R5.md", "Saldo insuficiente (validado", body)

    st, _, body = http_request(
        "POST",
        f"{URL}/contas/1291/saque",
        token=ctx.token_cliente,
        json_body={"valor": "10.00"},
    )
    _catalog(ctx, "R5 201", "TX-R5.md", "Saque registrado", body)
    poll_saldo_http(ctx, f"/clientes/{CPF_CATHARYNA}/conta", "800.00", ctx.token_cliente)

    st, _, body = http_request(
        "POST",
        f"{URL}/contas/1291/transferencia",
        token=ctx.token_cliente,
        json_body={"contaDestino": "0950", "valor": "100.00"},
    )
    _catalog(ctx, "R6 201", "TX-R6.md", "Transferência registrada", body)
    poll_saldo_http(ctx, f"/clientes/{CPF_CATHARYNA}/conta", "700.00", ctx.token_cliente)

    st, _, body = http_request("GET", f"{URL}/contas/1291/extrato", token=ctx.token_cliente)
    _catalog(ctx, "R7 pós mov", "TX-R7.md", "Últimos 30 dias após T04–T06", body)

    st, _, body = http_request(
        "GET",
        f"{URL}/contas/1291/extrato",
        token=ctx.token_cliente,
        query={"inicio": "2020-01-01", "fim": "2021-01-02"},
    )
    _expect_status(ctx, "R7 422", st, 422, body)
    _catalog(ctx, "R7 422", "TX-R7.md", "Intervalo maior que 365 dias", body)

    # --- TX-R10 ---
    ctx.note("== TX-R10 rejeição ==")
    st, _, body = http_request(
        "POST",
        f"{URL}/solicitacoes/{CPF_NOVO}/rejeicao",
        token=ctx.token_gerente,
        json_body={"motivo": "Renda incompatível com a política do banco"},
    )
    _catalog(ctx, "R10 200", "TX-R10.md", "Response 200 — Rejeitada", body)

    # --- TX-R9 SAGA + e-mail outbox ---
    ctx.note("== TX-R9 aprovação SAGA + outbox ==")
    st, _, body = http_request("POST", f"{URL}/solicitacoes", json_body=SOLICITACAO_T09)
    _expect_status(ctx, "R9 solicitação", st, 201, body)
    _catalog(ctx, "R9 sol", "TX-R1.md", "Solicitação Beltrano (aceite T09)", body)

    st, hdrs, body = http_request(
        "POST",
        f"{URL}/solicitacoes/{CPF_T09}/aprovacao",
        token=ctx.token_gerente,
    )
    _expect_status(ctx, "R9 202", st, 202, body)
    _catalog(ctx, "R9 202", "TX-R9.md", "Response 202 — Job aceito", body)
    job_id = body["jobId"]
    ctx.job_ids.append(job_id)
    assert hdrs.get("location") == f"/jobs/{job_id}/status", hdrs

    job = poll_job_http(ctx, job_id, ctx.token_gerente, timeout_s=90)
    _catalog(ctx, "R9 job OK", "TX-R9.md", "Polling → GET /jobs/{jobId}/status → CONCLUIDO", job)

    pwd = senha_outbox(EMAIL_T09)
    ctx.note(f"  OK outbox senha para {EMAIL_T09} (MAIL_DEV)")

    st, _, body = http_request("GET", f"{URL}/clientes/{CPF_T09}", token=ctx.token_gerente)
    _catalog(ctx, "CAD Beltrano", "TX-CAD-01.md", "Cliente Beltrano (após TX-R9)", body)

    token_beltrano = login_http(ctx, EMAIL_T09, pwd)

    if check_rabbit and management_ready() and wait_queues_idle:
        wait_queues_idle(SAGA_QUEUES, max_ready=0, timeout_s=45)
        assert_dlq_clean()
        ctx.note("  OK RabbitMQ filas SAGA ociosas e DLQ limpa")

    # --- TX-R9 falha e-mail gerente ---
    st, _, body = http_request("POST", f"{URL}/solicitacoes", json_body=SOLICITACAO_T09_DUP)
    _expect_status(ctx, "dup sol", st, 201, body)
    st, _, body = http_request(
        "POST",
        f"{URL}/solicitacoes/{CPF_T09_DUP}/aprovacao",
        token=ctx.token_gerente,
    )
    _expect_status(ctx, "dup aprov", st, 202, body)
    job_fail = poll_job_http(ctx, body["jobId"], ctx.token_gerente)
    _catalog(ctx, "R9 falha email", "TX-R9.md", "Job FALHA (e-mail já de gerente", job_fail)

    # --- TX-R12 pós estado / R11 ---
    ctx.note("== TX-R11 busca (pós movimentos) ==")
    st, _, body = http_request(
        "GET",
        f"{URL}/clientes",
        token=ctx.token_gerente,
        query={"busca": "Cat"},
    )
    _catalog(ctx, "R11", "TX-R11.md", "Com filtro", body)

    st, _, body = http_request("GET", f"{URL}/gerentes", token=ctx.token_gerente)
    _catalog(ctx, "R12", "TX-R12.md", "Response 200 — Seed", body)

    # --- TX-R13 SAGA gerente ---
    ctx.note("== TX-R13 inserir gerente ==")
    st, _, body = http_request("POST", f"{URL}/gerentes", token=ctx.token_gerente, json_body=GERENTE_T13)
    _expect_status(ctx, "R13 202", st, 202, body)
    _catalog(ctx, "R13 202", "TX-R13.md", "Response 202 — Job aceito", body)
    job13 = poll_job_http(ctx, body["jobId"], ctx.token_gerente, timeout_s=90)
    _catalog(ctx, "R13 job", "TX-R13.md", "Polling → CONCLUIDO resource", job13)

    st, _, body = http_request("GET", f"{URL}/gerentes/{GERENTE_T13['cpf']}", token=ctx.token_gerente)
    _catalog(ctx, "R13 GET", "TX-R13.md", "55667788990 — Recurso criado", body)

    st, _, body = http_request("POST", f"{URL}/gerentes", token=ctx.token_gerente, json_body=GERENTE_T13B)
    poll_job_http(ctx, body["jobId"], ctx.token_gerente, timeout_s=90)

    st, _, body = http_request("POST", f"{URL}/gerentes", token=ctx.token_gerente, json_body=GERENTE_T13_DUP)
    job_dup = poll_job_http(ctx, body["jobId"], ctx.token_gerente, timeout_s=90)
    _catalog(ctx, "R13 dup", "TX-R13.md", "Job FALHA — E-mail duplicado", job_dup)

    if check_rabbit and management_ready() and wait_queues_idle:
        wait_queues_idle(SAGA_QUEUES, max_ready=0, timeout_s=45)
        assert_dlq_clean(DLQ_QUEUES)
        ctx.note("  OK RabbitMQ após R13")

    # --- TX-R14 ---
    ctx.note("== TX-R14 atualizar gerente ==")
    st, _, body = http_request(
        "PUT",
        f"{URL}/gerentes/{CPF_GERENTE}",
        token=ctx.token_gerente,
        json_body={"nome": "Geniéve Silva", "telefone": "41988889999"},
    )
    _catalog(ctx, "R14 200", "TX-R14.md", "Atualizado", body)

    # --- TX-R15 ---
    ctx.note("== TX-R15 remover gerente ==")
    st, _, body = http_request("DELETE", f"{URL}/gerentes/{CPF_GERENTE}", token=ctx.token_gerente)
    _expect_status(ctx, "R15 self", st, 403, body)
    _catalog(ctx, "R15 403", "TX-R15.md", "Response 403 — sem job", body)

    st, _, acc15 = http_request("DELETE", f"{URL}/gerentes/{CPF_GADAMANTIO}", token=ctx.token_gerente)
    _expect_status(ctx, "R15 202", st, 202, acc15)
    _catalog(ctx, "R15 202", "TX-R15.md", "Response 202 — Job aceito", acc15)
    job15_id = acc15["jobId"]
    job15 = poll_job_http(ctx, job15_id, ctx.token_gerente, timeout_s=90)
    _catalog(ctx, "R15 inline", "TX-R15.md", "CONCLUIDO inline", job15)
    st, _, res15 = http_request(
        "GET",
        f"{URL}/jobs/{job15_id}/result",
        token=ctx.token_gerente,
    )
    _catalog(ctx, "JOB-02 R15", "TX-JOB-02.md", "Response 200 — Resultado R15", res15)

    # --- TX-R16 ---
    ctx.note("== TX-R16 relatório ==")
    st, _, acc16 = http_request("GET", f"{URL}/relatorios/clientes", token=ctx.token_gerente)
    _expect_status(ctx, "R16 202", st, 202, acc16)
    _catalog(ctx, "R16 202", "TX-R16.md", "Response 202 — Job aceito", acc16)
    job16_id = acc16["jobId"]
    job16 = poll_job_http(ctx, job16_id, ctx.token_gerente, timeout_s=90)
    _catalog(ctx, "R16 job", "TX-R16.md", "CONCLUIDO inline", job16)
    st, _, res16 = http_request(
        "GET",
        f"{URL}/jobs/{job16_id}/result",
        token=ctx.token_gerente,
    )
    heading = (
        "Lista após T09 (6 clientes)"
        if isinstance(res16, dict) and len(res16.get("clientes", [])) == 6
        else "GET /jobs/{jobId}/result — Lista"
    )
    _catalog(ctx, "R16 result", "TX-R16.md", heading, res16)

    st, _, body = http_request("GET", f"{URL}/relatorios/clientes", token=ctx.token_cliente)
    _expect_status(ctx, "R16 403", st, 403, body)
    _catalog(ctx, "R16 403", "TX-R16.md", "403 — CLIENTE autenticado", body)

    # --- TX-JOB-01 extras ---
    ctx.note("== TX-JOB-01 ==")
    st, _, body = http_request(
        "GET",
        f"{URL}/jobs/00000000-0000-0000-0000-000000000000/status",
        token=ctx.token_gerente,
    )
    _expect_status(ctx, "job 404", st, 404, body)
    _catalog(ctx, "JOB 404", "TX-JOB-01.md", "UUID inexistente", body)

    # --- TX-R2B logout ---
    ctx.note("== TX-R2B logout ==")
    st, _, body = http_request("POST", f"{URL}/logout", token=token_beltrano)
    _expect_status(ctx, "logout", st, 204, body)

    ctx.note("== Fluxo HTTPie E2E concluído ==")


def write_report(ctx: Ctx, ok: bool, extra: str = "") -> None:
    now = datetime.now(timezone.utc).astimezone().strftime("%Y-%m-%d %H:%M:%S %z")
    lines = [
        "# Relatório — HTTPie E2E (BANTADS)",
        "",
        f"Gerado em: **{now}**",
        f"Gateway: `{URL}`",
        f"Resultado: **{'PASSOU' if ok else 'FALHOU'}**",
        "",
        "## Passos",
        "",
    ]
    lines.extend(f"- {line}" for line in ctx.log)
    if extra:
        lines.extend(["", extra])
    lines.extend(
        [
            "",
            "## Como repetir",
            "",
            "```powershell",
            "cd httpie",
            "python run-transacoes-e2e.py",
            "```",
            "",
            "Tutoriais: pasta `httpie/TX-*.md` (equivalente a `transacoes/`, gitignored no clone).",
            "JSON: `json-catalog/TX-*.md` via `catalog_assert` (mesmo matcher dos contract-tests).",
            "",
        ]
    )
    REPORT_PATH.write_text("\n".join(lines), encoding="utf-8")
    print(f"Relatório: {REPORT_PATH}")


def main() -> int:
    parser = argparse.ArgumentParser(description="E2E BANTADS via HTTPie CLI")
    parser.add_argument("--skip-rabbit", action="store_true", help="Não checar RabbitMQ Management")
    parser.add_argument(
        "--with-pytest",
        action="store_true",
        help="Após HTTPie, rodar pytest em backend/contract-tests",
    )
    args = parser.parse_args()

    ctx = Ctx()
    ok = True
    try:
        wait_gateway(ctx)
        run_flow(ctx, check_rabbit=not args.skip_rabbit)
    except (AssertionError, HttpieCliError, SystemExit) as exc:
        ok = False
        ctx.note(f"FALHA: {exc}")
    except Exception as exc:
        ok = False
        ctx.note(f"ERRO: {exc}")

    extra = ""
    if args.with_pytest and ok:
        ctx.note("== Encadeando pytest contract-tests ==")
        proc = subprocess.run(
            [sys.executable, "-m", "pytest", "-q", "--tb=line"],
            cwd=CONTRACT,
        )
        extra = f"Pytest exit code: {proc.returncode}"
        ok = proc.returncode == 0

    write_report(ctx, ok, extra)
    return 0 if ok else 1


if __name__ == "__main__":
    raise SystemExit(main())
