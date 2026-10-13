import requests

from helpers import CPF_CATHARYNA, URL, auth_headers, login, login_cliente, poll_saldo


def test_t04_deposito():
    token = login_cliente()
    resp = requests.post(
        URL + "/contas/1291/deposito",
        headers=auth_headers(token),
        json={"valor": "10.00"},
        timeout=10,
    )
    assert resp.status_code == 201, resp.text
    body = resp.json()
    assert "saldo" not in body
    assert body["valor"] == "10.00"
    poll_saldo(f"/clientes/{CPF_CATHARYNA}/conta", "810.00", token)


def test_t05_saque():
    token = login_cliente()
    over = requests.post(
        URL + "/contas/1291/saque",
        headers=auth_headers(token),
        json={"valor": "900.00"},
        timeout=10,
    )
    assert over.status_code == 422, over.text
    ok = requests.post(
        URL + "/contas/1291/saque",
        headers=auth_headers(token),
        json={"valor": "10.00"},
        timeout=10,
    )
    assert ok.status_code == 201, ok.text
    assert "saldo" not in ok.json()
    poll_saldo(f"/clientes/{CPF_CATHARYNA}/conta", "800.00", token)


def test_t06_transferencia():
    token = login_cliente()
    same = requests.post(
        URL + "/contas/1291/transferencia",
        headers=auth_headers(token),
        json={"contaDestino": "1291", "valor": "100.00"},
        timeout=10,
    )
    assert same.status_code == 422, same.text
    missing = requests.post(
        URL + "/contas/1291/transferencia",
        headers=auth_headers(token),
        json={"contaDestino": "0001", "valor": "100.00"},
        timeout=10,
    )
    assert missing.status_code == 422, missing.text
    ok = requests.post(
        URL + "/contas/1291/transferencia",
        headers=auth_headers(token),
        json={"contaDestino": "0950", "valor": "100.00"},
        timeout=10,
    )
    assert ok.status_code == 201, ok.text
    body = ok.json()
    assert body["tipo"] == "TRANSFERENCIA"
    assert body["destino"]["numeroConta"] == "0950"
    assert "saldo" not in body
    poll_saldo(f"/clientes/{CPF_CATHARYNA}/conta", "700.00", token)
    poll_saldo("/contas/0950", "10100.00", login("cli2@bantads.com.br"))


def test_t07_extrato():
    token = login_cliente()
    resp = requests.get(URL + "/contas/1291/extrato", headers=auth_headers(token), timeout=10)
    assert resp.status_code == 200, resp.text
    body = resp.json()
    assert "saldoAbertura" in body
    assert "movimentacoes" in body
    assert body["_links"]
    too_long = requests.get(
        URL + "/contas/1291/extrato",
        headers=auth_headers(token),
        params={"inicio": "2020-01-01", "fim": "2021-01-02"},
        timeout=10,
    )
    assert too_long.status_code == 422, too_long.text
