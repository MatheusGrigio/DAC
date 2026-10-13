import requests

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
    assert resp.status_code == 202, resp.text
    accepted = resp.json()
    assert accepted["status"] == "PENDENTE"
    assert "_links" not in accepted
    assert resp.headers.get("Location") == f"/jobs/{accepted['jobId']}/status"
    return accepted


def test_t13_inserir_gerente():
    token = login_gerente()
    accepted = _inserir(token, GERENTE_T13)
    job = poll_job(accepted["jobId"], token)
    assert job["status"] == "CONCLUIDO", job
    assert job["resultType"] == "resource"
    assert job["dominio"] == "gerentes"
    assert job["resourceId"] == GERENTE_T13["cpf"]
    got = requests.get(
        URL + f"/gerentes/{GERENTE_T13['cpf']}",
        headers=auth_headers(token),
        timeout=10,
    )
    assert got.status_code == 200, got.text
    assert got.json()["nome"] == GERENTE_T13["nome"]
    assert got.json()["email"] == GERENTE_T13["email"]
    poll_conta_gerente(CONTA_COANDRYA, GERENTE_T13["cpf"], token)
    novo = login(GERENTE_T13["email"], GERENTE_T13["senha"])
    self_get = requests.get(
        URL + f"/gerentes/{GERENTE_T13['cpf']}",
        headers=auth_headers(novo),
        timeout=10,
    )
    assert self_get.status_code == 200, self_get.text


def test_t13_segundo_insert_e_email_duplicado():
    token = login_gerente()
    segundo = _inserir(token, GERENTE_T13B)
    job = poll_job(segundo["jobId"], token)
    assert job["status"] == "CONCLUIDO", job
    poll_conta_gerente(CONTA_CATHARYNA, GERENTE_T13B["cpf"], token)
    dup = _inserir(token, GERENTE_T13_DUP)
    falha = poll_job(dup["jobId"], token)
    assert falha["status"] == "FALHA", falha
    orphan = requests.get(
        URL + f"/gerentes/{GERENTE_T13_DUP['cpf']}",
        headers=auth_headers(token),
        timeout=10,
    )
    assert orphan.status_code == 404, orphan.text
