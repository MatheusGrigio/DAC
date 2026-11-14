import bantads as b


def R02_login_sucesso_1():
    r = b.post("/login", {"email": "cli1@bantads.com.br", "senha": "tads"})
    assert r.status_code == 200
    corpo = r.json()
    assert corpo["auth"] is True
    assert corpo["token"]  # JWT
    assert corpo["tipo"] == "CLIENTE"
    assert corpo["usuario"]["cpf"] == "12912861012"
    assert "senha" not in corpo["usuario"]
    # guarda o token no cache.json (como o front faria no LocalStorage)
    b.cache_set("token_cliente", corpo["token"])


def R02_login_invalido_2():
    # senha errada -> 401
    r = b.post("/login", {"email": "cli1@bantads.com.br", "senha": "errada"})
    assert r.status_code == 401


def R02_token_nao_fornecido_3():
    # endpoint protegido sem o header x-access-token -> 401
    r = b.get("/clientes")
    assert r.status_code == 401
    assert r.json()["auth"] is False


def R02_token_invalido_4():
    # token que não é um JWT válido -> 401 (verifyJWT: falha ao autenticar)
    r = b.get("/clientes", token="isto-nao-e-um-jwt")
    assert r.status_code == 401
    assert r.json()["auth"] is False


def R02_logout_5():
    # login próprio, descartável, para não invalidar o token guardado no cache
    tok = b.post("/login", {"email": "ger1@bantads.com.br", "senha": "tads"}).json()["token"]
    assert b.get("/gerentes", token=tok).status_code == 200  # token vale antes
    assert b.post("/logout", token=tok).status_code == 204
    # após o logout a sessão é apagada no Redis -> o mesmo token não vale mais
    assert b.get("/gerentes", token=tok).status_code == 401
