import requests

from catalog_assert import assert_catalog_json
from helpers import CPF_CATHARYNA, CPF_CLEUDDONIO, URL, login_cliente


def test_t03_own_account():
    token = login_cliente()
    resp = requests.get(
        URL + f"/clientes/{CPF_CATHARYNA}/conta",
        headers={"x-access-token": token},
        timeout=10,
    )
    assert resp.status_code == 200, resp.text
    assert_catalog_json("TX-R3A.md", "Response 200 — CLIENTE dono", resp.json())


def test_t03p_other_account_forbidden():
    token = login_cliente()
    resp = requests.get(
        URL + f"/clientes/{CPF_CLEUDDONIO}/conta",
        headers={"x-access-token": token},
        timeout=10,
    )
    assert resp.status_code == 403, resp.text
    assert_catalog_json("TX-R3A.md", "403 — CLIENTE tentando conta de outro", resp.json())
