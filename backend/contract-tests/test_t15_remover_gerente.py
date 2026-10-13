import requests

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
    assert resp.json()["status"] == 403


def test_t15_remove_gadamantio_e_inativo_nao_loga():
    token = login_gerente()
    accepted = requests.delete(
        URL + f"/gerentes/{CPF_GADAMANTIO}",
        headers=auth_headers(token),
        timeout=10,
    )
    assert accepted.status_code == 202, accepted.text
    body = accepted.json()
    assert body["status"] == "PENDENTE"
    assert "_links" not in body
    job = poll_job(body["jobId"], token)
    assert job["status"] == "CONCLUIDO", job
    assert job["resultType"] == "inline"
    result = requests.get(
        URL + f"/jobs/{body['jobId']}/result",
        headers=auth_headers(token),
        timeout=10,
    )
    assert result.status_code == 200, result.text
    mensagem = result.json()["mensagem"]
    assert mensagem.startswith("Gerente removido")
    assert "contas transferidas para" in mensagem
    assert "_links" not in result.json()
    login = requests.post(
        URL + "/login",
        headers=HEADERS,
        json={"email": EMAIL_GADAMANTIO, "senha": "tads"},
        timeout=10,
    )
    assert login.status_code == 401, login.text
    assert login.json() == {"auth": False, "message": "Login inválido!"}


def test_t15_inexistente_ou_ja_inativo_falha_no_job():
    token = login_gerente()
    again = requests.delete(
        URL + f"/gerentes/{CPF_GADAMANTIO}",
        headers=auth_headers(token),
        timeout=10,
    )
    assert again.status_code == 202, again.text
    job = poll_job(again.json()["jobId"], token)
    assert job["status"] == "FALHA", job
    missing = requests.delete(
        URL + "/gerentes/00000000000",
        headers=auth_headers(token),
        timeout=10,
    )
    assert missing.status_code == 202, missing.text
    missing_job = poll_job(missing.json()["jobId"], token)
    assert missing_job["status"] == "FALHA", missing_job
