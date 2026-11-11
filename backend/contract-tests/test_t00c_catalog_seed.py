"""Respostas de seed do json-catalog (após reboot T00, antes de mutações T01+)."""

import requests

from catalog_assert import assert_catalog_json, walk_dinheiro
from helpers import CPF_CATHARYNA, CPF_GERENTE, URL, auth_headers, login_cliente, login_gerente


def test_t00c_catalog_json_seed():
    gerente = login_gerente()
    cliente = login_cliente()

    cad = requests.get(
        URL + f"/clientes/{CPF_CATHARYNA}",
        headers=auth_headers(gerente),
        timeout=10,
    )
    assert cad.status_code == 200, cad.text
    assert_catalog_json("TX-CAD-01.md", "Response 200 — Cliente (seed Catharyna)", cad.json())

    lista = requests.get(
        URL + "/clientes",
        headers=auth_headers(gerente),
        params={"busca": "Cat"},
        timeout=10,
    )
    assert lista.status_code == 200, lista.text
    assert_catalog_json("TX-R11.md", "Response 200 — Com filtro", lista.json())

    conta_cli = requests.get(URL + "/contas/1291", headers=auth_headers(cliente), timeout=10)
    assert conta_cli.status_code == 200, conta_cli.text
    assert_catalog_json("TX-R3B.md", "Response 200 — CLIENTE dono", conta_cli.json())

    conta_ger = requests.get(URL + "/contas/1291", headers=auth_headers(gerente), timeout=10)
    assert conta_ger.status_code == 200, conta_ger.text
    assert_catalog_json("TX-R3A.md", "Response 200 — GERENTE", conta_ger.json())

    gerentes = requests.get(URL + "/gerentes", headers=auth_headers(gerente), timeout=10)
    assert gerentes.status_code == 200, gerentes.text
    assert_catalog_json("TX-R12.md", "Response 200 — Seed", gerentes.json())

    proprio = requests.get(URL + f"/gerentes/{CPF_GERENTE}", headers=auth_headers(gerente), timeout=10)
    assert proprio.status_code == 200, proprio.text
    assert_catalog_json("TX-CAD-02.md", "Response 200 — Próprio gerente autenticado", proprio.json())

    for body in (cad.json(), lista.json(), conta_cli.json(), gerentes.json()):
        walk_dinheiro(body)
