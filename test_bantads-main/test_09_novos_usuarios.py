"""Ciclo completo dos 4 usuários configurados no config.json:
2 gerentes novos (inserção, login, atividades) e 2 clientes novos (autocadastro,
aprovação, LOGIN e atividades).

O cliente novo, ao ser aprovado (R9), recebe uma senha aleatória "por e-mail". Ao
logar como cliente novo, a suíte PAUSA e pede que você digite essa senha (leia no
e-mail que o backend enviou). Sem terminal, esses logins são pulados (skip).
"""
import bantads as b

SENHA_GER = "tads"  # senha do gerente novo é informada na inserção (R13)


def _end():
    return {"logradouro": "Rua Nova", "numero": "1", "complemento": None,
            "cep": "80000000", "cidade": "Curitiba", "uf": "PR"}


def _saldo_str(cpf, tok):
    """Saldo da conta do cliente (string) ou None se ainda não projetada (CQRS)."""
    r = b.get(f"/clientes/{cpf}/conta", token=tok)
    return r.json()["saldo"] if r.status_code == 200 else None


def _relatorio_cpfs(tok):
    """Dispara o relatório (R16) e devolve o conjunto de CPFs do resultado."""
    r = b.get("/relatorios/clientes", token=tok)
    assert r.status_code == 202
    jid = r.json()["jobId"]
    assert b.poll_job(jid, tok)["resultType"] == "inline"
    return {c["cpf"] for c in b.get(f"/jobs/{jid}/result", token=tok).json()["clientes"]}


# ---------------- Gerentes novos: inserção, login, atividades ---------------- #
def R13_gerente_novo_inserir_1():
    cpf, email = b.novo_cpf(), b.CONFIG["novos_gerentes"][0]
    r = b.post("/gerentes", {"cpf": cpf, "nome": "Gerente Novo 1", "email": email,
                             "telefone": "41990000001", "senha": SENHA_GER},
               token=b.token("gerente"))
    assert r.status_code == 202
    job = b.poll_job(r.json()["jobId"], b.token("gerente"))
    assert job["status"] == "CONCLUIDO"
    assert job["resourceId"] == cpf
    b.cache_set("ger_novo_1", {"cpf": cpf, "email": email})


def R13_gerente_novo_inserir_2():
    cpf, email = b.novo_cpf(), b.CONFIG["novos_gerentes"][1]
    r = b.post("/gerentes", {"cpf": cpf, "nome": "Gerente Novo 2", "email": email,
                             "telefone": "41990000002", "senha": SENHA_GER},
               token=b.token("gerente"))
    assert r.status_code == 202
    assert b.poll_job(r.json()["jobId"], b.token("gerente"))["status"] == "CONCLUIDO"
    b.cache_set("ger_novo_2", {"cpf": cpf, "email": email})


def R02_gerente_novo_login_3():
    email = b.exigir("ger_novo_1")["email"]
    r = b.post("/login", {"email": email, "senha": SENHA_GER})
    assert r.status_code == 200
    assert r.json()["auth"] is True
    assert r.json()["tipo"] == "GERENTE"
    b.cache_set("token_ger_novo_1", r.json()["token"])


def R12_gerente_novo_lista_gerentes_4():
    tok = b.exigir("token_ger_novo_1")
    ativos = [g["cpf"] for g in b.get("/gerentes", token=tok).json()["gerentes"]]
    assert b.exigir("ger_novo_1")["cpf"] in ativos
    assert b.exigir("ger_novo_2")["cpf"] in ativos


# ---------------- Cliente novo 1: autocadastro -> aprovação -> login -> atividade ---------------- #
def R01_cliente_novo_autocadastro_5():
    cpf, email = b.novo_cpf(), b.CONFIG["novos_clientes"][0]
    r = b.post("/solicitacoes", {"cpf": cpf, "nome": "Cliente Novo 1", "email": email,
                                 "telefone": "41991000001", "salario": "3000.00",
                                 "endereco": _end()})
    assert r.status_code == 201
    assert r.json()["status"] == "PENDENTE"
    assert "aprovacao" in r.json()["_links"]
    b.cache_set("cli_novo_1", {"cpf": cpf, "email": email})


def R09_gerente_novo_aprova_cliente_6():
    # o gerente NOVO 1 aprova o cliente novo 1 (mostra gerente novo executando R9)
    cpf = b.exigir("cli_novo_1")["cpf"]
    tok = b.exigir("token_ger_novo_1")
    r = b.post(f"/solicitacoes/{cpf}/aprovacao", token=tok)
    assert r.status_code == 202
    job = b.poll_job(r.json()["jobId"], tok)
    assert job["status"] == "CONCLUIDO"
    assert job["dominio"] == "clientes"
    assert job["resourceId"] == cpf


def R02_login_cliente_novo_1_7():
    # login logo após a aprovação -> senha_recente.txt é a deste cliente
    dados = b.exigir("cli_novo_1")
    senha = b.obter_senha("cli_novo_1", dados["email"], dados["cpf"])
    r = b.post("/login", {"email": dados["email"], "senha": senha})
    assert r.status_code == 200, "senha incorreta? confira o console / senha_recente.txt"
    assert r.json()["auth"] is True
    assert r.json()["tipo"] == "CLIENTE"
    tok = r.json()["token"]
    b.cache_set("token_cli_novo_1", tok)
    # atividade (leitura): consulta a própria conta; o read model (CQRS) pode
    # demorar a projetar a conta recém-criada, então espera o saldo 0.00 aparecer
    b.poll_ate(lambda: _saldo_str(dados["cpf"], tok) == "0.00",
               descricao=f"conta do cliente {dados['cpf']} projetar (saldo 0.00)")


# ---------------- Cliente novo 2: autocadastro -> aprovação -> login -> atividade ---------------- #
def R01_cliente_novo_autocadastro_8():
    cpf, email = b.novo_cpf(), b.CONFIG["novos_clientes"][1]
    r = b.post("/solicitacoes", {"cpf": cpf, "nome": "Cliente Novo 2", "email": email,
                                 "telefone": "41991000002", "salario": "7000.00",
                                 "endereco": _end()})
    assert r.status_code == 201
    assert r.json()["status"] == "PENDENTE"
    b.cache_set("cli_novo_2", {"cpf": cpf, "email": email})


def R09_aprova_cliente_novo_9():
    # o cliente novo 2 é aprovado por um gerente do seed
    cpf = b.exigir("cli_novo_2")["cpf"]
    r = b.post(f"/solicitacoes/{cpf}/aprovacao", token=b.token("gerente"))
    assert r.status_code == 202
    assert b.poll_job(r.json()["jobId"], b.token("gerente"))["status"] == "CONCLUIDO"


def R02_login_cliente_novo_2_10():
    dados = b.exigir("cli_novo_2")
    senha = b.obter_senha("cli_novo_2", dados["email"], dados["cpf"])
    r = b.post("/login", {"email": dados["email"], "senha": senha})
    assert r.status_code == 200, "senha incorreta? confira o console / senha_recente.txt"
    assert r.json()["auth"] is True
    tok = r.json()["token"]
    b.cache_set("token_cli_novo_2", tok)
    # atividade (escrita): depósito na própria conta. Espera a conta projetar
    # (CQRS) para obter o número; depois reconsulta o saldo até refletir o depósito
    b.poll_ate(lambda: b.get(f"/clientes/{dados['cpf']}/conta", token=tok).status_code == 200,
               descricao="conta do cliente novo 2 projetar")
    numero = b.get(f"/clientes/{dados['cpf']}/conta", token=tok).json()["numero"]
    r = b.post(f"/contas/{numero}/deposito", {"valor": "100.00"}, token=tok)
    assert r.status_code == 201
    b.poll_saldo(numero, tok, "100.00")


# ---------------- Verificação dos clientes novos pelo lado do gerente ---------------- #
def R11_cliente_novo_na_listagem_11():
    tok = b.exigir("token_ger_novo_1")
    alvo = {b.exigir("cli_novo_1")["cpf"], b.exigir("cli_novo_2")["cpf"]}
    # a listagem (composition com o lado query) pode demorar a incluir os novos
    b.poll_ate(lambda: alvo <= {c["cpf"] for c in b.get("/clientes", token=tok).json()["clientes"]},
               descricao="clientes novos aparecerem na listagem")


def R16_cliente_novo_no_relatorio_12():
    tok = b.exigir("token_ger_novo_1")
    alvo = {b.exigir("cli_novo_1")["cpf"], b.exigir("cli_novo_2")["cpf"]}
    b.poll_ate(lambda: alvo <= _relatorio_cpfs(tok),
               descricao="clientes novos aparecerem no relatório")


# ---------------- Gerente novo 2: login e atividades (antes de ser removido) ---------------- #
def R02_gerente_novo_login_2_13():
    email = b.exigir("ger_novo_2")["email"]
    r = b.post("/login", {"email": email, "senha": SENHA_GER})
    assert r.status_code == 200
    assert r.json()["auth"] is True
    assert r.json()["tipo"] == "GERENTE"
    b.cache_set("token_ger_novo_2", r.json()["token"])


def R12_gerente_novo_2_lista_gerentes_14():
    tok = b.exigir("token_ger_novo_2")
    ativos = [g["cpf"] for g in b.get("/gerentes", token=tok).json()["gerentes"]]
    assert b.exigir("ger_novo_2")["cpf"] in ativos


def R11_gerente_novo_2_lista_clientes_15():
    tok = b.exigir("token_ger_novo_2")
    alvo = {b.exigir("cli_novo_1")["cpf"], b.exigir("cli_novo_2")["cpf"]}
    b.poll_ate(lambda: alvo <= {c["cpf"] for c in b.get("/clientes", token=tok).json()["clientes"]},
               descricao="clientes novos na listagem (gerente novo 2)")


def R16_gerente_novo_2_relatorio_16():
    tok = b.exigir("token_ger_novo_2")
    r = b.get("/relatorios/clientes", token=tok)
    assert r.status_code == 202
    job_id = r.json()["jobId"]
    assert b.poll_job(job_id, tok)["resultType"] == "inline"
    assert b.get(f"/jobs/{job_id}/result", token=tok).status_code == 200


# ---------------- Mais atividades de gerente: atualização e remoção ---------------- #
def R14_gerente_novo_atualizar_17():
    cpf = b.exigir("ger_novo_1")["cpf"]
    r = b.put(f"/gerentes/{cpf}", {"nome": "Gerente Novo 1 (editado)", "telefone": "41999999999"},
              token=b.token("gerente"))
    assert r.status_code == 200
    assert r.json()["nome"] == "Gerente Novo 1 (editado)"


def R15_gerente_novo_remover_18():
    tok = b.exigir("token_ger_novo_1")
    alvo = b.exigir("ger_novo_2")["cpf"]
    r = b.delete(f"/gerentes/{alvo}", token=tok)
    assert r.status_code == 202
    assert b.poll_job(r.json()["jobId"], tok)["status"] == "CONCLUIDO"
    ativos = [g["cpf"] for g in b.get("/gerentes", token=tok).json()["gerentes"]]
    assert alvo not in ativos
