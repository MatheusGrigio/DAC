import requests

from helpers import URL, auth_headers, login_gerente


def test_t12_lista_gerentes():
    token = login_gerente()
    resp = requests.get(URL + "/gerentes", headers=auth_headers(token), timeout=10)
    assert resp.status_code == 200, resp.text
    body = resp.json()
    gerentes = body["gerentes"]
    assert len(gerentes) == 4
    assert all(item["ativo"] is True for item in gerentes)
    nomes = [item["nome"] for item in gerentes]
    assert nomes[0] == "Gadamântio"
    assert nomes[2] == "Godophredo"
    assert nomes[3] == "Gyândula"
    assert nomes[1] in {"Geniéve", "Geniéve Silva"}
    por_cpf = {item["cpf"]: item["quantidadeClientes"] for item in gerentes}
    assert por_cpf["98574307084"] == 2
    assert por_cpf["64065268052"] == 2
    assert por_cpf["23862179060"] == 1
    assert por_cpf["40501740066"] in {0, 1}
    assert "_links" in body
    assert "self" in gerentes[0]["_links"]
