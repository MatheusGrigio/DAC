import requests

from catalog_assert import assert_catalog_json
from helpers import CPF_GERENTE, URL, auth_headers, login_gerente


def test_t14_atualiza_gerente():
    token = login_gerente()
    ok = requests.put(
        URL + f"/gerentes/{CPF_GERENTE}",
        headers=auth_headers(token),
        json={"nome": "Geniéve Silva", "telefone": "41988889999"},
        timeout=10,
    )
    assert ok.status_code == 200, ok.text
    assert_catalog_json("TX-R14.md", "Response 200 — Atualizado", ok.json())

    bad = requests.put(
        URL + f"/gerentes/{CPF_GERENTE}",
        headers=auth_headers(token),
        json={
            "nome": "Geniéve Silva",
            "telefone": "41988889999",
            "email": "outro@bantads.com.br",
        },
        timeout=10,
    )
    assert bad.status_code == 400, bad.text
    assert_catalog_json("TX-R14.md", "Tentativa de mudar e-mail ou CPF", bad.json())
