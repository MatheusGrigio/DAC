import re

import requests

from helpers import URL, auth_headers, login_gerente

DINHEIRO = r"^\d+\.\d{2}$"


def test_t11_busca_cat():
    token = login_gerente()
    resp = requests.get(
        URL + "/clientes",
        headers=auth_headers(token),
        params={"busca": "Cat"},
        timeout=10,
    )
    assert resp.status_code == 200, resp.text
    body = resp.json()
    nomes = [item["nome"] for item in body["clientes"]]
    assert nomes == ["Catharyna", "Catianna"]
    assert "Cleuddônio" not in nomes
    for item in body["clientes"]:
        assert item["cidade"]
        assert item["estado"]
        assert re.fullmatch(DINHEIRO, item["saldo"])
        assert "self" in item["_links"]
    assert "_links" in body
