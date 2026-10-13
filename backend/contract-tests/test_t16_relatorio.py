import re

import requests

from helpers import URL, auth_headers, login_cliente, login_gerente, poll_job

DINHEIRO = r"^\d+\.\d{2}$"

SEED = {
    "12912861012": {
        "nome": "Catharyna",
        "email": "cli1@bantads.com.br",
        "salario": "10000.00",
        "numeroConta": "1291",
    },
    "09506382000": {
        "nome": "Cleuddônio",
        "email": "cli2@bantads.com.br",
        "salario": "20000.00",
        "numeroConta": "0950",
    },
    "85733854057": {
        "nome": "Catianna",
        "email": "cli3@bantads.com.br",
        "salario": "3000.00",
        "numeroConta": "8573",
    },
    "58872160006": {
        "nome": "Cutardo",
        "email": "cli4@bantads.com.br",
        "salario": "500.00",
        "numeroConta": "5887",
    },
    "76179646090": {
        "nome": "Coândrya",
        "email": "cli5@bantads.com.br",
        "salario": "1500.00",
        "numeroConta": "7617",
    },
}

SEED_ORDER = ["Catharyna", "Catianna", "Cleuddônio", "Coândrya", "Cutardo"]


def test_t16_relatorio_clientes():
    token = login_gerente()
    accepted = requests.get(URL + "/relatorios/clientes", headers=auth_headers(token), timeout=10)
    assert accepted.status_code == 202, accepted.text
    body = accepted.json()
    assert body["status"] == "PENDENTE"
    assert "cpf" not in body
    assert "_links" not in body
    assert accepted.headers.get("Location") == f"/jobs/{body['jobId']}/status"
    job = poll_job(body["jobId"], token, timeout_s=5)
    assert job["status"] == "CONCLUIDO", job
    assert job["resultType"] == "inline"
    result = requests.get(
        URL + f"/jobs/{body['jobId']}/result",
        headers=auth_headers(token),
        timeout=10,
    )
    assert result.status_code == 200, result.text
    payload = result.json()
    assert "_links" not in payload
    clientes = payload["clientes"]
    assert len(clientes) >= 5
    nomes = [item["nome"] for item in clientes]
    positions = [nomes.index(nome) for nome in SEED_ORDER]
    assert positions == sorted(positions)
    por_cpf = {item["cpf"]: item for item in clientes}
    for cpf, expected in SEED.items():
        row = por_cpf[cpf]
        assert row["nome"] == expected["nome"]
        assert row["email"] == expected["email"]
        assert row["salario"] == expected["salario"]
        assert row["numeroConta"] == expected["numeroConta"]
        assert re.fullmatch(DINHEIRO, row["saldo"])
        assert re.fullmatch(r"^\d{11}$", row["cpfGerente"])
        assert row["nomeGerente"]
        assert "_links" not in row


def test_t16_cliente_nao_acessa():
    token = login_cliente()
    resp = requests.get(URL + "/relatorios/clientes", headers=auth_headers(token), timeout=10)
    assert resp.status_code == 403, resp.text
