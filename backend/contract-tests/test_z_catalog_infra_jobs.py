"""TX-INFRA-*, TX-JOB-01/02, TX-R2B — catálogo json-catalog."""

import uuid

import requests

from catalog_assert import (
    assert_catalog_json,
    assert_job_accepted,
)
from helpers import CPF_T09, HEADERS, URL, auth_headers, login_gerente, poll_job


def test_tx_infra_01_health():
    resp = requests.get(URL + "/health", headers=HEADERS, timeout=10)
    assert resp.status_code == 200
    assert_catalog_json("TX-INFRA-01.md", "Response 200 — Gateway no ar", resp.json())


def test_tx_infra_02_reboot_body():
    resp = requests.post(URL + "/reboot", headers=HEADERS, timeout=90)
    assert resp.status_code == 200
    assert_catalog_json("TX-INFRA-02.md", "Response 200 — Seed recriado", resp.json())


def test_tx_job_01_404_inexistente():
    token = login_gerente()
    missing = requests.get(
        URL + f"/jobs/{uuid.uuid4()}/status",
        headers=auth_headers(token),
        timeout=10,
    )
    assert missing.status_code == 404
    assert_catalog_json("TX-JOB-01.md", "404 — UUID inexistente", missing.json())


def test_tx_job_01_403_outro_usuario():
    token = login_gerente()
    accepted = requests.post(
        URL + f"/solicitacoes/{CPF_T09}/aprovacao",
        headers=auth_headers(token),
        timeout=10,
    )
    if accepted.status_code != 202:
        requests.post(URL + "/solicitacoes", headers=HEADERS, json={
            "cpf": CPF_T09,
            "nome": "Beltrano",
            "email": "beltrano2@exemplo.com.br",
            "telefone": "41999990000",
            "salario": "4500.00",
            "endereco": {
                "logradouro": "Rua XV",
                "numero": "1",
                "complemento": None,
                "cep": "80060000",
                "cidade": "Curitiba",
                "uf": "PR",
            },
        }, timeout=10)
        accepted = requests.post(
            URL + f"/solicitacoes/{CPF_T09}/aprovacao",
            headers=auth_headers(token),
            timeout=10,
        )
    assert accepted.status_code == 202, accepted.text
    job_id = accepted.json()["jobId"]
    cliente = requests.post(
        URL + "/login",
        headers=HEADERS,
        json={"email": "cli1@bantads.com.br", "senha": "tads"},
        timeout=10,
    ).json()["token"]
    foreign = requests.get(
        URL + f"/jobs/{job_id}/status",
        headers=auth_headers(cliente),
        timeout=10,
    )
    assert foreign.status_code == 403
    assert_catalog_json("TX-JOB-01.md", "403 — Outro usuário", foreign.json())


def test_tx_job_02_409_result_nao_inline():
    token = login_gerente()
    accepted = requests.post(
        URL + f"/solicitacoes/{CPF_T09}/aprovacao",
        headers=auth_headers(token),
        timeout=10,
    )
    assert accepted.status_code == 202, accepted.text
    job_id = accepted.json()["jobId"]
    early = requests.get(
        URL + f"/jobs/{job_id}/result",
        headers=auth_headers(token),
        timeout=10,
    )
    if early.status_code == 200:
        poll_job(job_id, token)
    else:
        assert early.status_code == 409
        assert_catalog_json("TX-JOB-02.md", "409 — Job não concluído", early.json())


def test_tx_r2b_logout_204():
    token = login_gerente()
    logout = requests.post(URL + "/logout", headers=auth_headers(token), timeout=10)
    assert logout.status_code == 204
    assert logout.text == ""
