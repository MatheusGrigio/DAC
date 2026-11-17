"""Cenários HTTP → bloco json-catalog (heading) para test_catalog_exact_json."""

from __future__ import annotations

from dataclasses import dataclass
from typing import Callable

import requests

from helpers import (
    CPF_CATHARYNA,
    CPF_GADAMANTIO,
    CPF_GERENTE,
    CPF_NOVO,
    GERENTE_T13_DUP,
    HEADERS,
    NOVA_SOLICITACAO,
    SOLICITACAO_T09_DUP,
    URL,
    auth_headers,
    login_cliente,
    login_gerente,
    poll_job,
)


@dataclass(frozen=True)
class CatalogScenario:
    id: str
    tx_file: str
    heading: str
    status: int
    run: Callable[[], tuple[int, dict]]


def _resp(method: str, path: str, **kwargs) -> requests.Response:
    return requests.request(method, URL + path, timeout=kwargs.pop("timeout", 15), **kwargs)


def _json(method: str, path: str, **kwargs) -> tuple[int, dict]:
    r = _resp(method, path, **kwargs)
    if r.status_code == 204:
        return r.status_code, {}
    return r.status_code, r.json()


def all_scenarios() -> list[CatalogScenario]:
    scenarios: list[CatalogScenario] = []

    def add(sid: str, tx: str, heading: str, status: int, run: Callable[[], tuple[int, dict]]) -> None:
        scenarios.append(CatalogScenario(sid, tx, heading, status, run))

    add("INFRA-01-health", "TX-INFRA-01.md", "Response 200 — Gateway no ar", 200, lambda: _json("GET", "/health", headers=HEADERS))
    add(
        "INFRA-02-reboot",
        "TX-INFRA-02.md",
        "Response 200 — Seed recriado",
        200,
        lambda: _json("POST", "/reboot", headers=HEADERS, timeout=90),
    )

    add(
        "R2A-login-cliente",
        "TX-R2A.md",
        "Response 200 — Autenticado (CLIENTE)",
        200,
        lambda: _json(
            "POST",
            "/login",
            headers={**HEADERS, "Content-Type": "application/json"},
            json={"email": "cli1@bantads.com.br", "senha": "tads"},
        ),
    )
    add(
        "R2A-login-gerente",
        "TX-R2A.md",
        "Response 200 — Autenticado (GERENTE)",
        200,
        lambda: _json(
            "POST",
            "/login",
            headers={**HEADERS, "Content-Type": "application/json"},
            json={"email": "ger1@bantads.com.br", "senha": "tads"},
        ),
    )
    add(
        "R2A-login-invalido",
        "TX-R2A.md",
        "401 — Credenciais inválidas",
        401,
        lambda: _json(
            "POST",
            "/login",
            headers={**HEADERS, "Content-Type": "application/json"},
            json={"email": "x@y.com", "senha": "wrong"},
        ),
    )

    add(
        "R1-201",
        "TX-R1.md",
        "Response 201 — Solicitação registrada",
        201,
        lambda: _json("POST", "/solicitacoes", headers={**HEADERS, "Content-Type": "application/json"}, json=NOVA_SOLICITACAO),
    )

    def r1_409_cpf() -> tuple[int, dict]:
        _json("POST", "/solicitacoes", headers={**HEADERS, "Content-Type": "application/json"}, json=NOVA_SOLICITACAO)
        return _json("POST", "/solicitacoes", headers={**HEADERS, "Content-Type": "application/json"}, json=NOVA_SOLICITACAO)

    add("R1-409-cpf", "TX-R1.md", "409 — CPF já possui solicitação", 409, r1_409_cpf)

    add(
        "CAD-01-catharyna",
        "TX-CAD-01.md",
        "Response 200 — Cliente (seed Catharyna)",
        200,
        lambda: _json("GET", f"/clientes/{CPF_CATHARYNA}", headers=auth_headers(login_gerente())),
    )

    add(
        "CAD-02-proprio",
        "TX-CAD-02.md",
        "Response 200 — Próprio gerente autenticado",
        200,
        lambda: _json("GET", f"/gerentes/{CPF_GERENTE}", headers=auth_headers(login_gerente())),
    )
    add(
        "CAD-02-gadamantio",
        "TX-CAD-02.md",
        "Response 200 — Outro gerente",
        200,
        lambda: _json("GET", f"/gerentes/{CPF_GADAMANTIO}", headers=auth_headers(login_gerente())),
    )
    add(
        "CAD-02-404",
        "TX-CAD-02.md",
        "404 — CPF inexistente",
        404,
        lambda: _json("GET", "/gerentes/00000000000", headers=auth_headers(login_gerente())),
    )

    add(
        "R3A-conta-cpf",
        "TX-R3A.md",
        "Response 200 — CLIENTE dono",
        200,
        lambda: _json("GET", f"/clientes/{CPF_CATHARYNA}/conta", headers=auth_headers(login_cliente())),
    )

    add(
        "R3B-conta-numero",
        "TX-R3B.md",
        "Response 200 — CLIENTE dono",
        200,
        lambda: _json("GET", "/contas/1291", headers=auth_headers(login_cliente())),
    )

    add(
        "R7-extrato-jan2020",
        "TX-R7.md",
        "Response 200 — Janeiro/2020 seed da Catharyna",
        200,
        lambda: _json(
            "GET",
            "/contas/1291/extrato",
            headers=auth_headers(login_cliente()),
            params={"inicio": "2020-01-01", "fim": "2020-01-31"},
        ),
    )

    add(
        "R7-422-intervalo",
        "TX-R7.md",
        "422 — Intervalo maior que 365 dias",
        422,
        lambda: _json(
            "GET",
            "/contas/1291/extrato",
            headers=auth_headers(login_cliente()),
            params={"inicio": "2020-01-01", "fim": "2021-01-02"},
        ),
    )

    def r8b_pendente() -> tuple[int, dict]:
        _json("POST", "/solicitacoes", headers={**HEADERS, "Content-Type": "application/json"}, json=NOVA_SOLICITACAO)
        return _json("GET", f"/solicitacoes/{CPF_NOVO}", headers=auth_headers(login_gerente()))

    add("R8B-pendente", "TX-R8B.md", "Response 200 — Solicitação PENDENTE", 200, r8b_pendente)

    add(
        "R11-busca-cat",
        "TX-R11.md",
        "Response 200 — Com filtro",
        200,
        lambda: _json("GET", "/clientes", headers=auth_headers(login_gerente()), params={"busca": "Cat"}),
    )

    add(
        "R12-lista",
        "TX-R12.md",
        "Response 200 — Seed",
        200,
        lambda: _json("GET", "/gerentes", headers=auth_headers(login_gerente())),
    )

    add(
        "JOB-01-404",
        "TX-JOB-01.md",
        "404 — UUID inexistente",
        404,
        lambda: _json(
            "GET",
            "/jobs/00000000-0000-0000-0000-000000000000/status",
            headers=auth_headers(login_gerente()),
        ),
    )

    def r13_falha() -> tuple[int, dict]:
        token = login_gerente()
        acc = _json("POST", "/gerentes", headers=auth_headers(token), json=GERENTE_T13_DUP)
        job_id = acc[1]["jobId"]
        poll_job(job_id, token)
        return _json("GET", f"/jobs/{job_id}/status", headers=auth_headers(token))

    add("R13-job-falha-email", "TX-R13.md", "Job FALHA — E-mail duplicado", 200, r13_falha)

    def r9_falha_email() -> tuple[int, dict]:
        _json("POST", "/solicitacoes", headers={**HEADERS, "Content-Type": "application/json"}, json=SOLICITACAO_T09_DUP)
        token = login_gerente()
        acc = _json("POST", f"/solicitacoes/{SOLICITACAO_T09_DUP['cpf']}/aprovacao", headers=auth_headers(token))
        poll_job(acc[1]["jobId"], token)
        return _json("GET", f"/jobs/{acc[1]['jobId']}/status", headers=auth_headers(token))

    add("R9-job-falha-email-gerente", "TX-R9.md", "Job FALHA (e-mail já de gerente", 200, r9_falha_email)

    return scenarios
