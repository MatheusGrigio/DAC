import requests

from catalog_assert import assert_catalog_json, assert_job_accepted
from helpers import URL, auth_headers, login_cliente, login_gerente, poll_job


def test_t16_relatorio_clientes():
    token = login_gerente()
    accepted = requests.get(URL + "/relatorios/clientes", headers=auth_headers(token), timeout=10)
    body = assert_job_accepted(accepted, "TX-R16.md", "Response 202 — Job aceito")
    job = poll_job(body["jobId"], token, timeout_s=60)
    assert_catalog_json("TX-R16.md", "CONCLUIDO inline", job)
    result = requests.get(
        URL + f"/jobs/{body['jobId']}/result",
        headers=auth_headers(token),
        timeout=10,
    )
    assert result.status_code == 200, result.text
    payload = result.json()
    heading = (
        "Lista após T09 (6 clientes)"
        if len(payload.get("clientes", [])) == 6
        else "GET /jobs/{jobId}/result — Lista"
    )
    assert_catalog_json("TX-R16.md", heading, payload)


def test_t16_cliente_nao_acessa():
    token = login_cliente()
    resp = requests.get(URL + "/relatorios/clientes", headers=auth_headers(token), timeout=10)
    assert resp.status_code == 403, resp.text
    assert_catalog_json("TX-R16.md", "403 — CLIENTE autenticado", resp.json())
