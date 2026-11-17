import requests

from catalog_assert import assert_catalog_json
from helpers import CPF_NOVO, URL, auth_headers, login_gerente


def test_t08_lista_solicitacoes():
    token = login_gerente()
    resp = requests.get(URL + "/solicitacoes", headers=auth_headers(token), timeout=10)
    assert resp.status_code == 200, resp.text
    assert_catalog_json("TX-R8A.md", "Lista com item PENDENTE", resp.json())


def test_t10_rejeicao():
    token = login_gerente()
    resp = requests.post(
        URL + f"/solicitacoes/{CPF_NOVO}/rejeicao",
        headers=auth_headers(token),
        json={"motivo": "Renda incompatível com a política do banco"},
        timeout=10,
    )
    assert resp.status_code == 200, resp.text
    assert_catalog_json("TX-R10.md", "Response 200 — Rejeitada", resp.json())
    again = requests.post(
        URL + f"/solicitacoes/{CPF_NOVO}/rejeicao",
        headers=auth_headers(token),
        json={"motivo": "outra vez"},
        timeout=10,
    )
    assert again.status_code == 409, again.text
    assert_catalog_json("TX-R10.md", "Já processada (rejeitar de novo)", again.json())
