import requests

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
    assert resp.status_code == 202, resp.text
    body = resp.json()
    assert body["status"] == "PENDENTE"
    assert "_links" not in body
    job_id = body["jobId"]
    assert resp.headers.get("Location") == f"/jobs/{job_id}/status"
    return body


def test_t09_aprovacao():
    created = requests.post(URL + "/solicitacoes", headers=HEADERS, json=SOLICITACAO_T09, timeout=10)
    assert created.status_code == 201, created.text
    token = login_gerente()
    accepted = _aprovar(token, CPF_T09)
    job = poll_job(accepted["jobId"], token)
    assert job["status"] == "CONCLUIDO", job
    assert job["resultType"] == "resource"
    assert job["dominio"] == "clientes"
    assert job["resourceId"] == CPF_T09
    cliente = requests.get(URL + f"/clientes/{CPF_T09}", headers=auth_headers(token), timeout=10)
    assert cliente.status_code == 200, cliente.text
    assert cliente.json()["cpf"] == CPF_T09
    senha = senha_outbox(EMAIL_T09)
    novo = login(EMAIL_T09, senha)
    self_get = requests.get(URL + f"/clientes/{CPF_T09}", headers=auth_headers(novo), timeout=10)
    assert self_get.status_code == 200, self_get.text


def test_t09f_sem_prevalidacao():
    token = login_gerente()
    again = _aprovar(token, CPF_T09)
    job_again = poll_job(again["jobId"], token)
    assert job_again["status"] == "FALHA", job_again
    missing = _aprovar(token, CPF_INEXISTENTE)
    job_missing = poll_job(missing["jobId"], token)
    assert job_missing["status"] == "FALHA", job_missing


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
    assert job["status"] == "FALHA", job
    solicitacao = requests.get(
        URL + f"/solicitacoes/{CPF_T09_DUP}",
        headers=auth_headers(token),
        timeout=10,
    )
    assert solicitacao.status_code == 200, solicitacao.text
    body = solicitacao.json()
    assert body["status"] == "NAO_APROVADA"
    assert body["motivo"] == "E-mail já cadastrado"
