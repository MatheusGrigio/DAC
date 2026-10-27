import requests

from helpers import CPF_NOVO, HEADERS, NOVA_SOLICITACAO, URL


def test_t01_autocadastro():
    resp = requests.post(URL + "/solicitacoes", headers=HEADERS, json=NOVA_SOLICITACAO, timeout=10)
    assert resp.status_code == 201, resp.text
    assert resp.headers.get("Location") == f"/solicitacoes/{CPF_NOVO}"
    body = resp.json()
    assert body["cpf"] == CPF_NOVO
    assert body["status"] == "PENDENTE"
    assert body["_links"]["self"]["href"].endswith(f"/solicitacoes/{CPF_NOVO}")
    assert "aprovacao" in body["_links"]
    assert "rejeicao" in body["_links"]


def test_t01d_duplicate_cpf_or_email():
    again = requests.post(URL + "/solicitacoes", headers=HEADERS, json=NOVA_SOLICITACAO, timeout=10)
    assert again.status_code == 409, again.text
    other = dict(NOVA_SOLICITACAO)
    other["cpf"] = "22233344405"
    other["nome"] = "Outro Fulano"
    dup_email = requests.post(URL + "/solicitacoes", headers=HEADERS, json=other, timeout=10)
    assert dup_email.status_code == 409, dup_email.text
