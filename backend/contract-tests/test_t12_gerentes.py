import requests

from catalog_assert import assert_catalog_json
from helpers import URL, auth_headers, login_gerente


def test_t12_lista_gerentes():
    token = login_gerente()
    resp = requests.get(URL + "/gerentes", headers=auth_headers(token), timeout=10)
    assert resp.status_code == 200, resp.text
    assert_catalog_json("TX-R12.md", "Response 200 — Seed", resp.json())
