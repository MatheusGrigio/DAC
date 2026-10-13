import re

import requests

from helpers import CPF_CATHARYNA, CPF_GERENTE, URL, auth_headers, login_cliente, login_gerente

DINHEIRO = re.compile(r"^\d+\.\d{2}$")
MONEY_KEYS = {"salario", "saldo", "valor", "saldoAbertura"}


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


def _assert_public_hrefs(body):
    for href in _hrefs(body):
        assert href.startswith(URL), href
        assert ":808" not in href, href


def test_t_hash_hateoas_seed_e_excecoes():
    login = requests.post(
        URL + "/login",
        json={"email": "ger1@bantads.com.br", "senha": "tads"},
        timeout=10,
    )
    assert login.status_code == 200
    assert "_links" not in login.json()
    gerente = login.json()["token"]
    cliente = login_cliente()

    health = requests.get(URL + "/health", timeout=10)
    assert health.status_code == 200
    assert "_links" not in health.json()

    cadastro = requests.get(URL + f"/clientes/{CPF_CATHARYNA}", headers=auth_headers(gerente), timeout=10)
    assert cadastro.status_code == 200, cadastro.text
    body = cadastro.json()
    for campo in ("cpf", "nome", "email", "telefone", "salario", "endereco", "_links"):
        assert campo in body
    assert "self" in body["_links"]
    assert "conta" in body["_links"]
    _assert_public_hrefs(body)

    lista = requests.get(
        URL + "/clientes",
        headers=auth_headers(gerente),
        params={"busca": "Cat"},
        timeout=10,
    )
    assert lista.status_code == 200, lista.text
    assert "_links" in lista.json()
    assert "busca=Cat" in lista.json()["_links"]["self"]["href"]
    assert "conta" in lista.json()["clientes"][0]["_links"]

    conta_cli = requests.get(URL + "/contas/1291", headers=auth_headers(cliente), timeout=10)
    assert conta_cli.status_code == 200, conta_cli.text
    links_cli = conta_cli.json()["_links"]
    for rel in ("self", "cliente", "deposito", "saque", "transferencia", "extrato"):
        assert rel in links_cli
    _assert_public_hrefs(conta_cli.json())

    conta_ger = requests.get(URL + "/contas/1291", headers=auth_headers(gerente), timeout=10)
    assert conta_ger.status_code == 200, conta_ger.text
    links_ger = conta_ger.json()["_links"]
    assert "self" in links_ger
    assert "cliente" in links_ger
    assert "deposito" not in links_ger
    assert "saque" not in links_ger
    assert "transferencia" not in links_ger

    gerentes = requests.get(URL + "/gerentes", headers=auth_headers(gerente), timeout=10)
    assert gerentes.status_code == 200, gerentes.text
    lista_g = gerentes.json()
    assert "self" in lista_g["_links"]
    assert "criacao" in lista_g["_links"]
    for item in lista_g["gerentes"]:
        assert "self" in item["_links"]
        if item["cpf"] == CPF_GERENTE:
            assert "remocao" not in item["_links"]
            assert "atualizacao" in item["_links"]
        elif item.get("ativo"):
            assert "remocao" in item["_links"]
    _assert_public_hrefs(lista_g)

    proprio = requests.get(URL + f"/gerentes/{CPF_GERENTE}", headers=auth_headers(gerente), timeout=10)
    assert proprio.status_code == 200, proprio.text
    assert "remocao" not in proprio.json()["_links"]
    assert "atualizacao" in proprio.json()["_links"]


def test_t_dollar_dinheiro_string():
    gerente = login_gerente()
    cliente = login_cliente()
    bodies = [
        requests.get(URL + f"/clientes/{CPF_CATHARYNA}", headers=auth_headers(gerente), timeout=10).json(),
        requests.get(URL + "/contas/1291", headers=auth_headers(cliente), timeout=10).json(),
        requests.get(URL + "/contas/1291/extrato", headers=auth_headers(cliente), timeout=10).json(),
        requests.get(URL + "/clientes", headers=auth_headers(gerente), params={"busca": "Cat"}, timeout=10).json(),
        requests.get(URL + "/gerentes", headers=auth_headers(gerente), timeout=10).json(),
    ]

    def walk(node):
        if isinstance(node, dict):
            for key, value in node.items():
                if key in MONEY_KEYS and value is not None:
                    assert isinstance(value, str), f"{key}={value!r}"
                    assert DINHEIRO.fullmatch(value), f"{key}={value!r}"
                walk(value)
        elif isinstance(node, list):
            for item in node:
                walk(item)

    for body in bodies:
        walk(body)
