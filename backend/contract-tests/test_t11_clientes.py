import requests

from catalog_assert import assert_catalog_json
from helpers import URL, auth_headers, login_gerente


def test_t11_busca_cat():
    token = login_gerente()
    resp = requests.get(
        URL + "/clientes",
        headers=auth_headers(token),
        params={"busca": "Cat"},
        timeout=10,
    )
    assert resp.status_code == 200, resp.text
    assert_catalog_json("TX-R11.md", "Response 200 — Com filtro", resp.json())
