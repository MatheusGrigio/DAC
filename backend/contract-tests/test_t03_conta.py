import requests

from helpers import CPF_CATHARYNA, CPF_CLEUDDONIO, URL, login_cliente, poll_saldo


def test_t03_own_account():
    token = login_cliente()
    resp = requests.get(
        URL + f"/clientes/{CPF_CATHARYNA}/conta",
        headers={"x-access-token": token},
        timeout=10,
    )
    assert resp.status_code == 200, resp.text
    body = resp.json()
    assert body["numero"] == "1291"
    assert body["saldo"] == "800.00"
    assert "deposito" in body["_links"]
    assert "saque" in body["_links"]
    assert "transferencia" in body["_links"]
    assert "extrato" in body["_links"]


def test_t03p_other_account_forbidden():
    token = login_cliente()
    resp = requests.get(
        URL + f"/clientes/{CPF_CLEUDDONIO}/conta",
        headers={"x-access-token": token},
        timeout=10,
    )
    assert resp.status_code == 403, resp.text
