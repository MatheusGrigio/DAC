import time

import requests

from catalog_assert import assert_catalog_json, assert_job_accepted
from helpers import (
    CPF_INEXISTENTE,
    CPF_T09,
    CPF_T09_DUP,
    EMAIL_T09,
    HEADERS,
    SOLICITACAO_T09,
    SOLICITACAO_T09_DUP,
    URL,
    auth_headers,
    login,
    login_gerente,
    poll_job,
    senha_outbox,
)


def _aprovar(token: str, cpf: str) -> dict:
    resp = requests.post(
        URL + f"/solicitacoes/{cpf}/aprovacao",
        headers=auth_headers(token),
        timeout=10,
    )
    return assert_job_accepted(resp)


def test_t09_aprovacao():
    created = requests.post(URL + "/solicitacoes", headers=HEADERS, json=SOLICITACAO_T09, timeout=10)
    assert created.status_code == 201, created.text
    assert_catalog_json("TX-R1.md", "Solicitação Beltrano (aceite T09)", created.json())
    token = login_gerente()
    accepted = _aprovar(token, CPF_T09)
    job = poll_job(accepted["jobId"], token)
    assert_catalog_json("TX-R9.md", "Polling → GET /jobs/{jobId}/status → CONCLUIDO", job)
    cliente = requests.get(URL + f"/clientes/{CPF_T09}", headers=auth_headers(token), timeout=10)
    assert cliente.status_code == 200, cliente.text
    assert_catalog_json("TX-CAD-01.md", "Cliente Beltrano (após TX-R9)", cliente.json())
    senha = senha_outbox(EMAIL_T09)
    novo = login(EMAIL_T09, senha)
    self_get = requests.get(URL + f"/clientes/{CPF_T09}", headers=auth_headers(novo), timeout=10)
    assert self_get.status_code == 200, self_get.text
    assert_catalog_json("TX-CAD-01.md", "Cliente Beltrano (após TX-R9)", self_get.json())


def test_t09f_sem_prevalidacao():
    token = login_gerente()
    again = _aprovar(token, CPF_T09)
    job_again = poll_job(again["jobId"], token)
    assert_catalog_json("TX-R9.md", "Job FALHA (CPF inexistente", job_again)
    missing = _aprovar(token, CPF_INEXISTENTE)
    job_missing = poll_job(missing["jobId"], token)
    assert_catalog_json("TX-R9.md", "Job FALHA (CPF inexistente", job_missing)


def test_t09f_email_de_gerente():
    created = requests.post(
        URL + "/solicitacoes",
        headers=HEADERS,
        json=SOLICITACAO_T09_DUP,
        timeout=10,
    )
    assert created.status_code == 201, created.text
    token = login_gerente()
    accepted = _aprovar(token, CPF_T09_DUP)
    job = poll_job(accepted["jobId"], token)
    assert_catalog_json("TX-R9.md", "Job FALHA (e-mail já de gerente", job)
    body = {}
    deadline = time.time() + 15
    while time.time() < deadline:
        solicitacao = requests.get(
            URL + f"/solicitacoes/{CPF_T09_DUP}",
            headers=auth_headers(token),
            timeout=10,
        )
        assert solicitacao.status_code == 200, solicitacao.text
        body = solicitacao.json()
        if body.get("status") == "NAO_APROVADA":
            break
        time.sleep(0.5)
    assert_catalog_json("TX-R8B.md", "NAO_APROVADA (falha R9 — e-mail de gerente)", body)
