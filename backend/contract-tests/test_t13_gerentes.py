import requests

from catalog_assert import assert_catalog_json, assert_job_accepted
from helpers import (
    CONTA_CATHARYNA,
    CONTA_COANDRYA,
    GERENTE_T13,
    GERENTE_T13_DUP,
    GERENTE_T13B,
    URL,
    auth_headers,
    login,
    login_gerente,
    poll_conta_gerente,
    poll_job,
)


def _inserir(token: str, body: dict) -> dict:
    resp = requests.post(URL + "/gerentes", headers=auth_headers(token), json=body, timeout=10)
    return assert_job_accepted(resp, "TX-R13.md", "Response 202 — Job aceito")


def test_t13_inserir_gerente():
    token = login_gerente()
    accepted = _inserir(token, GERENTE_T13)
    job = poll_job(accepted["jobId"], token)
    assert_catalog_json("TX-R13.md", "Polling → CONCLUIDO resource", job)
    got = requests.get(
        URL + f"/gerentes/{GERENTE_T13['cpf']}",
        headers=auth_headers(token),
        timeout=10,
    )
    assert got.status_code == 200, got.text
    assert_catalog_json("TX-R13.md", "55667788990 — Recurso criado", got.json())
    poll_conta_gerente(CONTA_COANDRYA, GERENTE_T13["cpf"], token)
    novo = login(GERENTE_T13["email"], GERENTE_T13["senha"])
    self_get = requests.get(
        URL + f"/gerentes/{GERENTE_T13['cpf']}",
        headers=auth_headers(novo),
        timeout=10,
    )
    assert self_get.status_code == 200, self_get.text
    assert_catalog_json("TX-R13.md", "Gumercindo autenticado (sem `remocao`)", self_get.json())


def test_t13_segundo_insert_e_email_duplicado():
    token = login_gerente()
    segundo = _inserir(token, GERENTE_T13B)
    job = poll_job(segundo["jobId"], token)
    assert_catalog_json("TX-R13.md", "Polling → CONCLUIDO resource", job)
    poll_conta_gerente(CONTA_CATHARYNA, GERENTE_T13B["cpf"], token)
    dup = _inserir(token, GERENTE_T13_DUP)
    falha = poll_job(dup["jobId"], token)
    assert_catalog_json("TX-R13.md", "Job FALHA — E-mail duplicado", falha)
    orphan = requests.get(
        URL + f"/gerentes/{GERENTE_T13_DUP['cpf']}",
        headers=auth_headers(token),
        timeout=10,
    )
    assert orphan.status_code == 404, orphan.text
    assert_catalog_json("TX-CAD-02.md", "404 — CPF inexistente", orphan.json())
