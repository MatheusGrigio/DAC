"""TX-R1, TX-R8A/B, TX-R10 — catálogo."""

import requests

from catalog_assert import assert_catalog_json
from helpers import (
    CPF_CATHARYNA,
    CPF_NOVO,
    HEADERS,
    NOVA_SOLICITACAO,
    URL,
    auth_headers,
    login_cliente,
    login_gerente,
)


def test_tx_r1_201_pendente_links():
    resp = requests.post(URL + "/solicitacoes", headers=HEADERS, json=NOVA_SOLICITACAO, timeout=10)
    assert resp.status_code == 201, resp.text
    assert_catalog_json("TX-R1.md", "Response 201 — Solicitação registrada", resp.json())


def test_tx_r1_409_cpf_solicitacao():
    resp = requests.post(URL + "/solicitacoes", headers=HEADERS, json=NOVA_SOLICITACAO, timeout=10)
    assert resp.status_code == 409
    assert_catalog_json("TX-R1.md", "409 — CPF já possui solicitação", resp.json())


def test_tx_r1_409_email_solicitacao():
    other = dict(NOVA_SOLICITACAO)
    other["cpf"] = "99988877766"
    other["nome"] = "Outro"
    resp = requests.post(URL + "/solicitacoes", headers=HEADERS, json=other, timeout=10)
    assert resp.status_code == 409
    assert_catalog_json("TX-R1.md", "409 — E-mail já usado em outra solicitação", resp.json())


def test_tx_r1_409_cpf_ja_cliente():
    dup = dict(NOVA_SOLICITACAO)
    dup["cpf"] = CPF_CATHARYNA
    dup["email"] = "novoemail@exemplo.com.br"
    resp = requests.post(URL + "/solicitacoes", headers=HEADERS, json=dup, timeout=10)
    assert resp.status_code == 409
    assert_catalog_json("TX-R1.md", "409 — CPF já é cliente do seed", resp.json())


def test_tx_r1_400_malformado():
    bad = dict(NOVA_SOLICITACAO)
    bad["cpf"] = "99988877766"
    bad["email"] = "mal@exemplo.com"
    bad["salario"] = 4500.00
    resp = requests.post(URL + "/solicitacoes", headers=HEADERS, json=bad, timeout=10)
    assert resp.status_code == 400
    assert_catalog_json("TX-R1.md", "400 — Salário como number", resp.json())


def test_tx_r8b_a_200_pendente():
    token = login_gerente()
    resp = requests.get(URL + f"/solicitacoes/{CPF_NOVO}", headers=auth_headers(token), timeout=10)
    assert resp.status_code == 200, resp.text
    assert_catalog_json("TX-R8B.md", "Response 200 — Solicitação PENDENTE", resp.json())


def test_tx_r8b_404_inexistente():
    token = login_gerente()
    resp = requests.get(URL + "/solicitacoes/00000000000", headers=auth_headers(token), timeout=10)
    assert resp.status_code == 404
    assert_catalog_json("TX-R8B.md", "404 — CPF sem solicitação", resp.json())


def test_tx_r8b_403_cliente():
    token = login_cliente()
    resp = requests.get(URL + f"/solicitacoes/{CPF_NOVO}", headers=auth_headers(token), timeout=10)
    assert resp.status_code == 403
    assert_catalog_json("TX-R8B.md", "403 — CLIENTE autenticado", resp.json())


def test_tx_r8a_lista_self_link():
    token = login_gerente()
    resp = requests.get(URL + "/solicitacoes", headers=auth_headers(token), timeout=10)
    assert resp.status_code == 200
    assert_catalog_json("TX-R8A.md", "Lista com item PENDENTE", resp.json())


def test_tx_r10_b_400_motivo_vazio():
    token = login_gerente()
    resp = requests.post(
        URL + f"/solicitacoes/{CPF_NOVO}/rejeicao",
        headers=auth_headers(token),
        json={"motivo": ""},
        timeout=10,
    )
    assert resp.status_code == 400
    assert_catalog_json("TX-R10.md", "motivo vazio", resp.json())


def test_tx_r10_c_200_nao_aprovada_shape():
    token = login_gerente()
    resp = requests.post(
        URL + f"/solicitacoes/{CPF_NOVO}/rejeicao",
        headers=auth_headers(token),
        json={"motivo": "Renda incompatível com a política do banco"},
        timeout=10,
    )
    assert resp.status_code == 200, resp.text
    assert_catalog_json("TX-R10.md", "Response 200 — Rejeitada", resp.json())


def test_tx_r8b_d_200_nao_aprovada():
    token = login_gerente()
    resp = requests.get(URL + f"/solicitacoes/{CPF_NOVO}", headers=auth_headers(token), timeout=10)
    assert resp.status_code == 200
    assert_catalog_json("TX-R8B.md", "Solicitação NAO_APROVADA (após TX-R10)", resp.json())
