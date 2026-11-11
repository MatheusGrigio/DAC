import requests

from catalog_assert import assert_catalog_json, assert_job_accepted
from helpers import (
    CPF_GADAMANTIO,
    CPF_GERENTE,
    EMAIL_GADAMANTIO,
    HEADERS,
    URL,
    auth_headers,
    login_gerente,
    poll_job,
)


def test_t15_nao_remove_a_si_mesmo():
    token = login_gerente()
    resp = requests.delete(URL + f"/gerentes/{CPF_GERENTE}", headers=auth_headers(token), timeout=10)
    assert resp.status_code == 403, resp.text
    assert_catalog_json("TX-R15.md", "Response 403 — sem job", resp.json())


def test_t15_remove_gadamantio_e_inativo_nao_loga():
    token = login_gerente()
    accepted = requests.delete(
        URL + f"/gerentes/{CPF_GADAMANTIO}",
        headers=auth_headers(token),
        timeout=10,
    )
    body = assert_job_accepted(accepted, "TX-R15.md", "Response 202 — Job aceito")
    job = poll_job(body["jobId"], token)
    assert_catalog_json("TX-R15.md", "CONCLUIDO inline", job)
    result = requests.get(
        URL + f"/jobs/{body['jobId']}/result",
        headers=auth_headers(token),
        timeout=10,
    )
    assert result.status_code == 200, result.text
    assert_catalog_json("TX-JOB-02.md", "Response 200 — Resultado R15", result.json())
    login = requests.post(
        URL + "/login",
        headers=HEADERS,
        json={"email": EMAIL_GADAMANTIO, "senha": "tads"},
        timeout=10,
    )
    assert login.status_code == 401, login.text
    assert_catalog_json("TX-R2A.md", "401 — Credenciais inválidas", login.json())
    atualizacao = requests.put(
        URL + f"/gerentes/{CPF_GADAMANTIO}",
        headers=auth_headers(token),
        json={"nome": "Gadamântio", "telefone": "41999990000"},
        timeout=10,
    )
    assert atualizacao.status_code == 409, atualizacao.text
    assert_catalog_json("TX-R14.md", "409 — Gerente inativo", atualizacao.json())


def test_t15_inexistente_ou_ja_inativo_falha_no_job():
    token = login_gerente()
    again = requests.delete(
        URL + f"/gerentes/{CPF_GADAMANTIO}",
        headers=auth_headers(token),
        timeout=10,
    )
    body = assert_job_accepted(again, "TX-R15.md", "Response 202 — Job aceito")
    job = poll_job(body["jobId"], token)
    assert_catalog_json("TX-R15.md", "Job FALHA (já inativo", job)
    missing = requests.delete(
        URL + "/gerentes/00000000000",
        headers=auth_headers(token),
        timeout=10,
    )
    missing_body = assert_job_accepted(missing, "TX-R15.md", "Response 202 — Job aceito")
    missing_job = poll_job(missing_body["jobId"], token)
    assert_catalog_json("TX-R15.md", "Job FALHA (já inativo", missing_job)
