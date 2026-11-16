"""Testes de acesso às contas dos CLIENTES NOVOS (espelho de test_05, mas com os
clientes criados/logados em test_09). Usa os tokens e contas do cache; se os
logins de cliente novo não aconteceram (ex.: modo interativo sem terminal), estes
testes pulam via `exigir`.

Saldos conferidos por DIFERENÇA (antes/depois), então a ordem não quebra os valores.
"""
import bantads as b


def _conta_tok(chave):
    """(numero_conta, token) do cliente novo, a partir do cache (login em test_09)."""
    dados = b.exigir(chave)                # {cpf, email}
    tok = b.exigir(f"token_{chave}")       # token do login
    numero = b.get(f"/clientes/{dados['cpf']}/conta", token=tok).json()["numero"]
    return numero, tok


# ---------------- Posse: um cliente não acessa a conta de outro ---------------- #
def R03_cliente_novo_conta_alheia_negada_1():
    # cli_novo_1 tenta ver a conta do cli_novo_2 -> 403
    cpf2 = b.exigir("cli_novo_2")["cpf"]
    tok1 = b.exigir("token_cli_novo_1")
    assert b.get(f"/clientes/{cpf2}/conta", token=tok1).status_code == 403


def R04_cliente_novo_deposito_conta_alheia_negado_2():
    # cli_novo_1 tenta depositar na conta do cli_novo_2 -> 403
    numero2, _ = _conta_tok("cli_novo_2")
    tok1 = b.exigir("token_cli_novo_1")
    assert b.post(f"/contas/{numero2}/deposito", {"valor": "10.00"}, token=tok1).status_code == 403


# ---------------- Depósito / Saque na própria conta ---------------- #
def R04_cliente_novo_deposito_3():
    numero, tok = _conta_tok("cli_novo_1")
    antes = b.saldo_conta(numero, tok)
    r = b.post(f"/contas/{numero}/deposito", {"valor": "200.00"}, token=tok)
    assert r.status_code == 201
    b.poll_saldo(numero, tok, antes + b.dec("200.00"))  # reconsulta (CQRS)


def R05_cliente_novo_saque_4():
    numero, tok = _conta_tok("cli_novo_1")
    antes = b.saldo_conta(numero, tok)
    r = b.post(f"/contas/{numero}/saque", {"valor": "50.00"}, token=tok)
    assert r.status_code == 201
    b.poll_saldo(numero, tok, antes - b.dec("50.00"))


def R05_cliente_novo_saque_insuficiente_5():
    numero, tok = _conta_tok("cli_novo_1")
    assert b.post(f"/contas/{numero}/saque", {"valor": "9999999.00"}, token=tok).status_code == 422


# ---------------- Transferência entre os dois clientes novos ---------------- #
def R06_cliente_novo_transferencia_6():
    numero1, tok1 = _conta_tok("cli_novo_1")
    numero2, tok2 = _conta_tok("cli_novo_2")
    orig_antes = b.saldo_conta(numero1, tok1)
    dest_antes = b.saldo_conta(numero2, tok2)
    r = b.post(f"/contas/{numero1}/transferencia",
               {"contaDestino": numero2, "valor": "30.00"}, token=tok1)
    assert r.status_code == 201
    assert r.json()["destino"]["nome"] == "Cliente Novo 2"
    b.poll_saldo(numero1, tok1, orig_antes - b.dec("30.00"))
    b.poll_saldo(numero2, tok2, dest_antes + b.dec("30.00"))


def R06_cliente_novo_transferencia_para_si_7():
    numero1, tok1 = _conta_tok("cli_novo_1")
    assert b.post(f"/contas/{numero1}/transferencia",
                  {"contaDestino": numero1, "valor": "10.00"}, token=tok1).status_code == 422


def R06_cliente_novo_transferencia_conta_inexistente_8():
    numero1, tok1 = _conta_tok("cli_novo_1")
    assert b.post(f"/contas/{numero1}/transferencia",
                  {"contaDestino": "9999", "valor": "10.00"}, token=tok1).status_code == 422


# ---------------- Extrato ---------------- #
def R07_cliente_novo_extrato_9():
    numero1, tok1 = _conta_tok("cli_novo_1")
    assert b.get(f"/contas/{numero1}/extrato", token=tok1).status_code == 200
    assert "saldoAbertura" in b.get(f"/contas/{numero1}/extrato", token=tok1).json()
    # o histórico (read model) pode demorar a refletir as operações -> espera aparecer
    b.poll_ate(lambda: len(b.get(f"/contas/{numero1}/extrato", token=tok1).json()["movimentacoes"]) >= 1,
               descricao="movimentações do cliente novo no extrato")


def R07_cliente_novo_extrato_intervalo_10():
    numero1, tok1 = _conta_tok("cli_novo_1")
    r = b.get(f"/contas/{numero1}/extrato", token=tok1, inicio="2020-01-01", fim="2023-01-01")
    assert r.status_code == 422
