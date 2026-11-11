"""TX-CAD-01, TX-R3A/B, TX-R4–R7 — catálogo."""

import requests

from catalog_assert import assert_auth_401_catalog, assert_catalog_json
from helpers import CPF_CATHARYNA, CPF_CLEUDDONIO, URL, auth_headers, login_cliente, login_gerente


def test_tx_cad_01_gerente_200():
    token = login_gerente()
    resp = requests.get(URL + f"/clientes/{CPF_CATHARYNA}", headers=auth_headers(token), timeout=10)
    assert resp.status_code == 200, resp.text
    assert_catalog_json("TX-CAD-01.md", "Response 200 — Cliente (seed Catharyna)", resp.json())


def test_tx_cad_01_cliente_proprio():
    token = login_cliente()
    resp = requests.get(URL + f"/clientes/{CPF_CATHARYNA}", headers=auth_headers(token), timeout=10)
    assert resp.status_code == 200, resp.text
    assert_catalog_json("TX-CAD-01.md", "Response 200 — Cliente (seed Catharyna)", resp.json())


def test_tx_cad_01_403_outro_cpf():
    token = login_cliente()
    resp = requests.get(URL + f"/clientes/{CPF_CLEUDDONIO}", headers=auth_headers(token), timeout=10)
    assert resp.status_code == 403
    assert_catalog_json("TX-CAD-01.md", "403 — CLIENTE consultando outro CPF", resp.json())


def test_tx_cad_01_404_nao_cliente():
    token = login_gerente()
    resp = requests.get(URL + "/clientes/00000000000", headers=auth_headers(token), timeout=10)
    assert resp.status_code == 404
    assert_catalog_json("TX-CAD-01.md", "404 — CPF não é cliente", resp.json())


def test_tx_r3a_conta_cpf():
    token = login_cliente()
    resp = requests.get(URL + f"/clientes/{CPF_CATHARYNA}/conta", headers=auth_headers(token), timeout=10)
    assert resp.status_code == 200
    assert_catalog_json("TX-R3A.md", "Response 200 — CLIENTE dono", resp.json())


def test_tx_r3b_conta_numero_cliente():
    token = login_cliente()
    resp = requests.get(URL + "/contas/1291", headers=auth_headers(token), timeout=10)
    assert resp.status_code == 200
    assert_catalog_json("TX-R3B.md", "Response 200 — CLIENTE dono", resp.json())


def test_tx_r3b_403_conta_outro_cliente():
    token = login_cliente()
    resp = requests.get(URL + "/contas/0950", headers=auth_headers(token), timeout=10)
    assert resp.status_code == 403
    assert_catalog_json("TX-R3B.md", "Erro 403 — CLIENTE de outra conta", resp.json())


def test_tx_r3b_gerente_sem_rels_operacao():
    token = login_gerente()
    resp = requests.get(URL + "/contas/1291", headers=auth_headers(token), timeout=10)
    assert resp.status_code == 200
    assert_catalog_json("TX-R3A.md", "Response 200 — GERENTE", resp.json())


def test_tx_r4_400_valor_number():
    token = login_cliente()
    resp = requests.post(
        URL + "/contas/1291/deposito",
        headers=auth_headers(token),
        json={"valor": 10},
        timeout=10,
    )
    assert resp.status_code == 400
    assert_catalog_json("TX-R4.md", "Erro 400 — Valor como number", resp.json())


def test_tx_r4_403_gerente_nao_deposita():
    token = login_gerente()
    resp = requests.post(
        URL + "/contas/1291/deposito",
        headers=auth_headers(token),
        json={"valor": "1.00"},
        timeout=10,
    )
    assert resp.status_code == 403
    assert_catalog_json("TX-R4.md", "Erro 403 — GERENTE ou conta alheia", resp.json())


def test_tx_r5_422_saldo_insuficiente():
    token = login_cliente()
    resp = requests.post(
        URL + "/contas/1291/saque",
        headers=auth_headers(token),
        json={"valor": "99999.00"},
        timeout=10,
    )
    assert resp.status_code == 422
    assert_catalog_json("TX-R5.md", "422 — Saldo insuficiente (validado", resp.json())


def test_tx_r6_transferencia_sem_token():
    resp = requests.post(
        URL + "/contas/1291/transferencia",
        headers={"Accept": "*/*", "Content-Type": "application/json"},
        json={"contaDestino": "0950", "valor": "1.00"},
        timeout=10,
    )
    assert resp.status_code == 401
    assert_auth_401_catalog(resp.json(), "TX-R2A.md", "401 — Token ausente")


def test_tx_r7_extrato_query():
    token = login_cliente()
    resp = requests.get(
        URL + "/contas/1291/extrato",
        headers=auth_headers(token),
        params={"inicio": "2020-01-01", "fim": "2020-01-31"},
        timeout=10,
    )
    assert resp.status_code == 200, resp.text
    assert_catalog_json("TX-R7.md", "Janeiro/2020 seed da Catharyna", resp.json())
