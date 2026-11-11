import requests

from helpers import URL


def _hrefs(node):
    found = []
    if isinstance(node, dict):
        href = node.get("href")
        if isinstance(href, str):
            found.append(href)
        for value in node.values():
            found.extend(_hrefs(value))
    elif isinstance(node, list):
        for item in node:
            found.extend(_hrefs(item))
    return found


def test_t_hash_hateoas_excecoes_sem_links():
    login = requests.post(
        URL + "/login",
        json={"email": "ger1@bantads.com.br", "senha": "tads"},
        timeout=10,
    )
    assert login.status_code == 200
    assert "_links" not in login.json()

    health = requests.get(URL + "/health", timeout=10)
    assert health.status_code == 200
    assert "_links" not in health.json()

    gerente = login.json()["token"]
    cadastro = requests.get(
        URL + "/clientes/12912861012",
        headers={"x-access-token": gerente, "Accept": "*/*"},
        timeout=10,
    )
    assert cadastro.status_code == 200, cadastro.text
    for href in _hrefs(cadastro.json()):
        assert href.startswith(URL), href
        assert ":808" not in href, href
