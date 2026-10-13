import requests

from helpers import HEADERS, URL, auth_headers, salvar_token


def test_t02a_without_token():
    resp = requests.get(URL + "/clientes/12912861012", headers=HEADERS, timeout=10)
    assert resp.status_code == 401
    assert resp.json() == {"auth": False, "message": "Token não fornecido."}


def test_t02b_invalid_token():
    resp = requests.get(
        URL + "/clientes/12912861012",
        headers={**HEADERS, "x-access-token": "nao.e.jwt"},
        timeout=10,
    )
    assert resp.status_code == 401
    assert resp.json() == {"auth": False, "message": "Falha ao autenticar o token."}


def test_t02c_login_invalid():
    resp = requests.post(
        URL + "/login",
        headers=HEADERS,
        json={"email": "cli1@bantads.com.br", "senha": "errada"},
        timeout=10,
    )
    assert resp.status_code == 401
    assert resp.json() == {"auth": False, "message": "Login inválido!"}


def test_t02d_login_seed_cliente():
    resp = requests.post(
        URL + "/login",
        headers=HEADERS,
        json={"email": "cli1@bantads.com.br", "senha": "tads"},
        timeout=10,
    )
    assert resp.status_code == 200
    body = resp.json()
    assert body["auth"] is True
    assert body["tipo"] == "CLIENTE"
    assert body["usuario"]["cpf"] == "12912861012"
    assert "_links" not in body
    salvar_token(body["token"])


def test_t02e_login_seed_gerente():
    resp = requests.post(
        URL + "/login",
        headers=HEADERS,
        json={"email": "ger1@bantads.com.br", "senha": "tads"},
        timeout=10,
    )
    assert resp.status_code == 200
    body = resp.json()
    assert body["auth"] is True
    assert body["tipo"] == "GERENTE"
    assert body["usuario"]["cpf"] == "98574307084"
    assert "_links" not in body


def test_t02f_logout_then_reuse_token():
    login = requests.post(
        URL + "/login",
        headers=HEADERS,
        json={"email": "cli1@bantads.com.br", "senha": "tads"},
        timeout=10,
    )
    token = login.json()["token"]
    logout = requests.post(URL + "/logout", headers=auth_headers(token), timeout=10)
    assert logout.status_code == 204
    reuse = requests.get(
        URL + "/clientes/12912861012",
        headers=auth_headers(token),
        timeout=10,
    )
    assert reuse.status_code == 401
    assert reuse.json() == {"auth": False, "message": "Falha ao autenticar o token."}
