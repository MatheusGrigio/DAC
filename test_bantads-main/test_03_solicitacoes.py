import bantads as b


def _endereco():
    return {"logradouro": "Rua de Teste", "numero": "10", "complemento": None,
            "cep": "80000000", "cidade": "Curitiba", "uf": "PR"}


# ------------------------------- R01 Autocadastro ------------------------------- #
def R01_autocadastro_sucesso_1():
    corpo = {"cpf": b.novo_cpf(), "nome": "Fulano de Teste", "email": b.novo_email("fulano"),
             "telefone": "41999990000", "salario": "4500.00", "endereco": _endereco()}
    r = b.post("/solicitacoes", corpo)
    assert r.status_code == 201
    assert r.json()["status"] == "PENDENTE"
    assert "aprovacao" in r.json()["_links"]  # HATEOAS: PENDENTE traz o link de aprovar
    b.cache_set("solicitacao", corpo)  # guarda para R08/R09


def R01_autocadastro_cpf_duplicado_2():
    corpo = b.exigir("solicitacao")
    r = b.post("/solicitacoes", corpo)  # mesmo CPF
    assert r.status_code == 409


def R01_autocadastro_dados_invalidos_3():
    r = b.post("/solicitacoes", {"cpf": "123"})  # faltando campos / CPF inválido
    assert r.status_code == 400


# ------------------------------- R08 Tela do gerente ------------------------------- #
def R08_listar_solicitacoes_1():
    r = b.get("/solicitacoes", token=b.token("gerente"))
    assert r.status_code == 200
    assert isinstance(r.json()["solicitacoes"], list)


def R08_obter_solicitacao_2():
    cpf = b.exigir("solicitacao")["cpf"]
    r = b.get(f"/solicitacoes/{cpf}", token=b.token("gerente"))
    assert r.status_code == 200
    assert r.json()["cpf"] == cpf


# ------------------------------- R09 Aprovar cliente ------------------------------- #
def R09_aprovar_cliente_1():
    cpf = b.exigir("solicitacao")["cpf"]
    r = b.post(f"/solicitacoes/{cpf}/aprovacao", token=b.token("gerente"))
    assert r.status_code == 202
    job = b.poll_job(r.json()["jobId"], b.token("gerente"))
    assert job["status"] == "CONCLUIDO"
    assert job["dominio"] == "clientes"
    assert job["resourceId"] == cpf
    # o cliente passou a existir
    # read model do CQRS é assíncrono: espera o cliente/conta projetar
    b.poll_ate(lambda: b.get(f"/clientes/{cpf}", token=b.token("gerente")).status_code == 200,
               descricao=f"cliente {cpf} aparecer após aprovação")


def R09_aprovar_novamente_falha_2():
    cpf = b.exigir("solicitacao")["cpf"]
    r = b.post(f"/solicitacoes/{cpf}/aprovacao", token=b.token("gerente"))
    assert r.status_code == 202
    job = b.poll_job(r.json()["jobId"], b.token("gerente"))
    assert job["status"] == "FALHA"  # não está mais PENDENTE


# ------------------------------- R10 Rejeitar cliente ------------------------------- #
def R10_rejeitar_cliente_1():
    cpf = b.novo_cpf()
    b.post("/solicitacoes", {"cpf": cpf, "nome": "Beltrano", "email": b.novo_email("beltrano"),
                             "telefone": "41999990001", "salario": "800.00", "endereco": _endereco()})
    r = b.post(f"/solicitacoes/{cpf}/rejeicao", {"motivo": "renda insuficiente"},
               token=b.token("gerente"))
    assert r.status_code == 200
    assert r.json()["status"] == "NAO_APROVADA"
    assert r.json()["motivo"] == "renda insuficiente"


def R10_rejeitar_inexistente_2():
    r = b.post("/solicitacoes/00000000000/rejeicao", {"motivo": "x"}, token=b.token("gerente"))
    assert r.status_code == 404
