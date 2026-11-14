import bantads as b


# ------------------------------- R03 Tela inicial do cliente ------------------------------- #
def R03_conta_do_cliente_1():
    r = b.get("/clientes/12912861012/conta", token=b.token("cliente"))
    assert r.status_code == 200
    conta = r.json()
    assert conta["numero"]                 # mostra o número da conta
    assert "." in conta["saldo"]           # saldo como string decimal
    assert "deposito" in conta["_links"]   # HATEOAS com as operações


def R03_conta_de_outro_cliente_negada_2():
    # cli1 tentando ver a conta do cli2 -> 403
    r = b.get("/clientes/09506382000/conta", token=b.token("cliente"))
    assert r.status_code == 403


# ------------------------------- R11 Consultar todos os clientes ------------------------------- #
def R11_listar_clientes_1():
    r = b.get("/clientes", token=b.token("gerente"))
    assert r.status_code == 200
    nomes = [c["nome"] for c in r.json()["clientes"]]
    assert nomes == sorted(nomes, key=b.chave_ordenacao)  # ordenado por nome (pt-BR)


def R11_buscar_cliente_2():
    r = b.get("/clientes", token=b.token("gerente"), busca="cat")
    assert r.status_code == 200
    for c in r.json()["clientes"]:
        assert "cat" in c["nome"].lower() or "cat" in c["cpf"]


def R11_obter_cliente_3():
    r = b.get("/clientes/12912861012", token=b.token("gerente"))
    assert r.status_code == 200
    assert r.json()["nome"] == "Catharyna"
