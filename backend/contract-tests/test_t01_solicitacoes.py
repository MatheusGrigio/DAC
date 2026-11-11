import requests

from catalog_assert import assert_catalog_json
from helpers import CPF_NOVO, HEADERS, NOVA_SOLICITACAO, URL


def test_t01_autocadastro():
    resp = requests.post(URL + "/solicitacoes", headers=HEADERS, json=NOVA_SOLICITACAO, timeout=10)
    assert resp.status_code == 201, resp.text
    assert resp.headers.get("Location") == f"/solicitacoes/{CPF_NOVO}"
    assert_catalog_json("TX-R1.md", "Response 201 — Solicitação registrada", resp.json())


def test_t01d_duplicate_cpf_or_email():
    again = requests.post(URL + "/solicitacoes", headers=HEADERS, json=NOVA_SOLICITACAO, timeout=10)
    assert again.status_code == 409, again.text
    assert_catalog_json("TX-R1.md", "409 — CPF já possui solicitação", again.json())
    other = dict(NOVA_SOLICITACAO)
    other["cpf"] = "22233344405"
    other["nome"] = "Outro Fulano"
    dup_email = requests.post(URL + "/solicitacoes", headers=HEADERS, json=other, timeout=10)
    assert dup_email.status_code == 409, dup_email.text
    assert_catalog_json("TX-R1.md", "409 — E-mail já usado em outra solicitação", dup_email.json())
