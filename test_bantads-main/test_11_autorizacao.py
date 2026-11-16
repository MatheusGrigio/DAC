"""Autorização (acesso por papel) com os usuários NOVOS, usando os tokens do
cache (test_09). Gerente não tem conta: não faz operações de cliente (403), mas
pode CONSULTAR conta/extrato de qualquer cliente (200). E cliente não acessa
rota de gerente (403).

O token do gerente novo 1 sempre existe (login com senha conhecida). O token do
cliente novo depende do login (automático/interativo); sem ele, o teste do
cliente pula via `exigir`.
"""
import bantads as b

CONTA_SEED = "1291"  # conta de um cliente do seed (Catharyna)


# ---------------- Gerente NÃO faz operações de cliente -> 403 ---------------- #
def R04_gerente_novo_nao_deposita_1():
    tok = b.exigir("token_ger_novo_1")
    assert b.post(f"/contas/{CONTA_SEED}/deposito", {"valor": "10.00"}, token=tok).status_code == 403


def R05_gerente_novo_nao_saca_2():
    tok = b.exigir("token_ger_novo_1")
    assert b.post(f"/contas/{CONTA_SEED}/saque", {"valor": "10.00"}, token=tok).status_code == 403


def R06_gerente_novo_nao_transfere_3():
    tok = b.exigir("token_ger_novo_1")
    assert b.post(f"/contas/{CONTA_SEED}/transferencia",
                  {"contaDestino": "0950", "valor": "10.00"}, token=tok).status_code == 403


# ---------------- Gerente PODE consultar conta/extrato de qualquer cliente -> 200 ---------------- #
def R03_gerente_novo_ve_conta_4():
    tok = b.exigir("token_ger_novo_1")
    assert b.get(f"/contas/{CONTA_SEED}", token=tok).status_code == 200


def R07_gerente_novo_ve_extrato_5():
    tok = b.exigir("token_ger_novo_1")
    assert b.get(f"/contas/{CONTA_SEED}/extrato", token=tok).status_code == 200


# ---------------- Cliente NÃO acessa rota de gerente -> 403 ---------------- #
def R12_cliente_novo_nao_lista_gerentes_6():
    tok = b.exigir("token_cli_novo_1")
    assert b.get("/gerentes", token=tok).status_code == 403
