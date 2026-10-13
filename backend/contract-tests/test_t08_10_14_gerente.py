import requests

from helpers import CPF_GERENTE, CPF_NOVO, URL, auth_headers, login_gerente


def test_t08_lista_solicitacoes():
    token = login_gerente()
    resp = requests.get(URL + "/solicitacoes", headers=auth_headers(token), timeout=10)
    assert resp.status_code == 200, resp.text
    body = resp.json()
    found = next(item for item in body["solicitacoes"] if item["cpf"] == CPF_NOVO)
    assert found["status"] == "PENDENTE"
    assert "aprovacao" in found["_links"]
    assert "rejeicao" in found["_links"]
    aprovada = next((item for item in body["solicitacoes"] if item["status"] != "PENDENTE"), None)
    if aprovada:
        assert "aprovacao" not in aprovada.get("_links", {})
        assert "rejeicao" not in aprovada.get("_links", {})


def test_t10_rejeicao():
    token = login_gerente()
    resp = requests.post(
        URL + f"/solicitacoes/{CPF_NOVO}/rejeicao",
        headers=auth_headers(token),
        json={"motivo": "Renda incompatível com a política do banco"},
        timeout=10,
    )
    assert resp.status_code == 200, resp.text
    body = resp.json()
    assert body["status"] == "NAO_APROVADA"
    assert body["motivo"]
    assert "rejeicao" not in body["_links"]
    again = requests.post(
        URL + f"/solicitacoes/{CPF_NOVO}/rejeicao",
        headers=auth_headers(token),
        json={"motivo": "outra vez"},
        timeout=10,
    )
    assert again.status_code == 409, again.text


def test_t14_atualiza_gerente():
    token = login_gerente()
    ok = requests.put(
        URL + f"/gerentes/{CPF_GERENTE}",
        headers=auth_headers(token),
        json={"nome": "Geniéve Silva", "telefone": "41988889999"},
        timeout=10,
    )
    assert ok.status_code == 200, ok.text
    assert ok.json()["nome"] == "Geniéve Silva"
    assert ok.json()["telefone"] == "41988889999"
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
