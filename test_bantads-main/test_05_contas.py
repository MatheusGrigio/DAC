import bantads as b

CONTA = "1291"          # conta da Catharyna (cli1)
CPF_CLI = "12912861012"


# ------------------------------- R03 Consultar conta por número ------------------------------- #
def R03_obter_conta_por_numero_1():
    r = b.get(f"/contas/{CONTA}", token=b.token("cliente"))
    assert r.status_code == 200
    assert r.json()["numero"] == CONTA
    assert r.json()["cpfCliente"] == CPF_CLI


# ------------------------------- R04 Depósito ------------------------------- #
def R04_deposito_1():
    tok = b.token("cliente")
    antes = b.saldo_conta(CONTA, tok)
    r = b.post(f"/contas/{CONTA}/deposito", {"valor": "150.00"}, token=tok)
    assert r.status_code == 201
    assert "saldo" not in r.json()  # a operação não devolve o novo saldo
    b.poll_saldo(CONTA, tok, antes + b.dec("150.00"))  # reconsulta o lado query (CQRS)


def R04_deposito_em_conta_alheia_negado_2():
    # cli1 tentando depositar na conta do cli2 (0950) -> 403
    r = b.post("/contas/0950/deposito", {"valor": "10.00"}, token=b.token("cliente"))
    assert r.status_code == 403


# ------------------------------- R05 Saque ------------------------------- #
def R05_saque_1():
    tok = b.token("cliente")
    antes = b.saldo_conta(CONTA, tok)
    r = b.post(f"/contas/{CONTA}/saque", {"valor": "50.00"}, token=tok)
    assert r.status_code == 201
    b.poll_saldo(CONTA, tok, antes - b.dec("50.00"))


def R05_saque_saldo_insuficiente_2():
    r = b.post(f"/contas/{CONTA}/saque", {"valor": "99999999.00"}, token=b.token("cliente"))
    assert r.status_code == 422


# ------------------------------- R06 Transferência ------------------------------- #
def R06_transferencia_1():
    tok = b.token("cliente")
    ger = b.token("gerente")
    origem_antes = b.saldo_conta(CONTA, tok)
    destino_antes = b.saldo_conta("0950", ger)  # conta do cli2, lida pelo gerente
    r = b.post(f"/contas/{CONTA}/transferencia", {"contaDestino": "0950", "valor": "30.00"}, token=tok)
    assert r.status_code == 201
    assert r.json()["destino"]["nome"] == "Cleuddônio"  # nome enriquecido pelo Gateway
    b.poll_saldo(CONTA, tok, origem_antes - b.dec("30.00"))
    b.poll_saldo("0950", ger, destino_antes + b.dec("30.00"))


def R06_transferencia_conta_inexistente_2():
    r = b.post(f"/contas/{CONTA}/transferencia", {"contaDestino": "9999", "valor": "10.00"},
               token=b.token("cliente"))
    assert r.status_code == 422


def R06_transferencia_para_si_mesmo_3():
    r = b.post(f"/contas/{CONTA}/transferencia", {"contaDestino": CONTA, "valor": "10.00"},
               token=b.token("cliente"))
    assert r.status_code == 422


# ------------------------------- R07 Extrato ------------------------------- #
def R07_extrato_1():
    r = b.get(f"/contas/{CONTA}/extrato", token=b.token("cliente"),
              inicio="2020-01-01", fim="2020-12-31")
    assert r.status_code == 200
    corpo = r.json()
    assert "saldoAbertura" in corpo
    assert isinstance(corpo["movimentacoes"], list)


def R07_extrato_intervalo_maior_que_365_2():
    r = b.get(f"/contas/{CONTA}/extrato", token=b.token("cliente"),
              inicio="2020-01-01", fim="2023-01-01")
    assert r.status_code == 422
