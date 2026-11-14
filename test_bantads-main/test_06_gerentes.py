import bantads as b

GER1 = "98574307084"  # Geniéve (dono do token 'gerente')


# ------------------------------- R12 Listagem de gerentes ------------------------------- #
def R12_listar_gerentes_1():
    r = b.get("/gerentes", token=b.token("gerente"))
    assert r.status_code == 200
    gerentes = r.json()["gerentes"]
    assert len(gerentes) >= 1
    assert all(g["ativo"] for g in gerentes)
    nomes = [g["nome"] for g in gerentes]
    assert nomes == sorted(nomes, key=b.chave_ordenacao)


def R12_obter_gerente_2():
    r = b.get(f"/gerentes/{GER1}", token=b.token("gerente"))
    assert r.status_code == 200
    assert r.json()["cpf"] == GER1


# ------------------------------- R13 Inserção de gerente ------------------------------- #
def R13_inserir_gerente_1():
    cpf = b.novo_cpf()
    corpo = {"cpf": cpf, "nome": "Gerente Novo", "email": b.novo_email("ger"),
             "telefone": "41988887777", "senha": "tads"}
    r = b.post("/gerentes", corpo, token=b.token("gerente"))
    assert r.status_code == 202
    job = b.poll_job(r.json()["jobId"], b.token("gerente"))
    assert job["status"] == "CONCLUIDO"
    assert job["resourceId"] == cpf
    b.cache_set("gerente_novo", cpf)  # para R14/R15
    assert b.get(f"/gerentes/{cpf}", token=b.token("gerente")).status_code == 200


def R13_inserir_email_duplicado_2():
    corpo = {"cpf": b.novo_cpf(), "nome": "Colide", "email": "ger1@bantads.com.br",
             "telefone": "41000", "senha": "tads"}
    r = b.post("/gerentes", corpo, token=b.token("gerente"))
    assert r.status_code == 202
    job = b.poll_job(r.json()["jobId"], b.token("gerente"))
    assert job["status"] == "FALHA"  # e-mail já usado (dentro da SAGA)


# ------------------------------- R14 Atualização de gerente ------------------------------- #
def R14_atualizar_gerente_1():
    cpf = b.exigir("gerente_novo")
    r = b.put(f"/gerentes/{cpf}", {"nome": "Gerente Renomeado", "telefone": "41777"},
              token=b.token("gerente"))
    assert r.status_code == 200
    assert r.json()["nome"] == "Gerente Renomeado"


def R14_email_imutavel_2():
    cpf = b.exigir("gerente_novo")
    r = b.put(f"/gerentes/{cpf}", {"nome": "X", "telefone": "1", "email": "outro@bantads.com.br"},
              token=b.token("gerente"))
    assert r.status_code == 400


# ------------------------------- R15 Remoção de gerente ------------------------------- #
def R15_remover_gerente_1():
    cpf = b.exigir("gerente_novo")
    r = b.delete(f"/gerentes/{cpf}", token=b.token("gerente"))
    assert r.status_code == 202
    job = b.poll_job(r.json()["jobId"], b.token("gerente"))
    assert job["status"] == "CONCLUIDO"
    ativos = [g["cpf"] for g in b.get("/gerentes", token=b.token("gerente")).json()["gerentes"]]
    assert cpf not in ativos


def R15_remover_a_si_mesmo_2():
    # o gerente logado (ger1) tentando remover a si mesmo -> 403 (antes da SAGA)
    r = b.delete(f"/gerentes/{GER1}", token=b.token("gerente"))
    assert r.status_code == 403


def R15_logout_forcado_3():
    # ao remover um gerente, sua sessão é encerrada no Redis (logout forçado):
    # um token que ele tinha deixa de valer imediatamente.
    cpf, email = b.novo_cpf(), b.novo_email("efemero")
    r = b.post("/gerentes", {"cpf": cpf, "nome": "Efêmero", "email": email,
                             "telefone": "41000", "senha": "tads"}, token=b.token("gerente"))
    b.poll_job(r.json()["jobId"], b.token("gerente"))
    tok = b.post("/login", {"email": email, "senha": "tads"}).json()["token"]
    assert b.get("/gerentes", token=tok).status_code == 200  # token vale
    # ger1 remove esse gerente
    r = b.delete(f"/gerentes/{cpf}", token=b.token("gerente"))
    b.poll_job(r.json()["jobId"], b.token("gerente"))
    # logout forçado: o token do removido não vale mais
    assert b.get("/gerentes", token=tok).status_code == 401
