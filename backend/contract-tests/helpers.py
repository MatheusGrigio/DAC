import json
import os
import time
from pathlib import Path

import requests

ROOT = Path(__file__).resolve().parent
URL = os.getenv("URL", "http://localhost:3000").rstrip("/")
ARQUIVO_TOKEN = ROOT / os.getenv("ARQUIVO_TOKEN", ".token")
ARQUIVO_CACHE = ROOT / os.getenv("ARQUIVO_CACHE", ".cache.json")
HEADERS = {"Accept": "*/*"}

CPF_CATHARYNA = "12912861012"
CPF_CLEUDDONIO = "09506382000"
CPF_GERENTE = "98574307084"
CPF_GADAMANTIO = "40501740066"
EMAIL_GADAMANTIO = "ger4@bantads.com.br"
CPF_NOVO = "11122233396"

NOVA_SOLICITACAO = {
    "cpf": CPF_NOVO,
    "nome": "Fulano de Tal",
    "email": "fulano@exemplo.com.br",
    "telefone": "41999990000",
    "salario": "4500.00",
    "endereco": {
        "logradouro": "Rua XV de Novembro",
        "numero": "1299",
        "complemento": None,
        "cep": "80060000",
        "cidade": "Curitiba",
        "uf": "PR",
    },
}


def salvar_token(token: str) -> None:
    ARQUIVO_TOKEN.write_text(token, encoding="utf-8")


def recuperar_token() -> str:
    return ARQUIVO_TOKEN.read_text(encoding="utf-8").strip()


def salvar_cache(cache: dict) -> None:
    ARQUIVO_CACHE.write_text(json.dumps(cache), encoding="utf-8")


def recuperar_cache() -> dict:
    if not ARQUIVO_CACHE.exists():
        return {}
    return json.loads(ARQUIVO_CACHE.read_text(encoding="utf-8"))


def auth_headers(token: str | None = None) -> dict:
    value = token if token is not None else recuperar_token()
    return {**HEADERS, "x-access-token": value}


def login(email: str, senha: str = "tads") -> str:
    resp = requests.post(
        URL + "/login",
        headers=HEADERS,
        json={"email": email, "senha": senha},
        timeout=10,
    )
    assert resp.status_code == 200, resp.text
    token = resp.json()["token"]
    salvar_token(token)
    return token


def login_cliente() -> str:
    return login("cli1@bantads.com.br")


def login_gerente() -> str:
    return login("ger1@bantads.com.br")


def poll_saldo(path: str, expected: str, token: str) -> dict:
    time.sleep(2)
    body = {}
    for _ in range(4):
        resp = requests.get(URL + path, headers=auth_headers(token), timeout=10)
        assert resp.status_code == 200, resp.text
        body = resp.json()
        if body.get("saldo") == expected:
            return body
        time.sleep(5)
    raise AssertionError(f"saldo {expected} não projetado: {body}")


REPO_ROOT = ROOT.parent.parent
OUTBOX = Path(os.getenv("OUTBOX_DIR", str(REPO_ROOT / "outbox")))

CPF_T09 = "22233344405"
EMAIL_T09 = "beltrano@exemplo.com.br"
CPF_T09_DUP = "33344455516"
CPF_INEXISTENTE = "00000000000"

SOLICITACAO_T09 = {
    **NOVA_SOLICITACAO,
    "cpf": CPF_T09,
    "nome": "Beltrano de Tal",
    "email": EMAIL_T09,
}

SOLICITACAO_T09_DUP = {
    **NOVA_SOLICITACAO,
    "cpf": CPF_T09_DUP,
    "nome": "Email de Gerente",
    "email": "ger1@bantads.com.br",
}


def poll_job(job_id: str, token: str, timeout_s: float = 45) -> dict:
    deadline = time.time() + timeout_s
    last: dict = {}
    while time.time() < deadline:
        resp = requests.get(URL + f"/jobs/{job_id}/status", headers=auth_headers(token), timeout=10)
        assert resp.status_code == 200, resp.text
        last = resp.json()
        assert "_links" not in last
        if last.get("status") in {"CONCLUIDO", "FALHA"}:
            return last
        time.sleep(0.5)
    raise AssertionError(f"job {job_id} não terminou: {last}")


def senha_outbox(email: str, timeout_s: float = 20) -> str:
    path = OUTBOX / f"{email}.txt"
    deadline = time.time() + timeout_s
    while time.time() < deadline:
        if path.exists():
            text = path.read_text(encoding="utf-8")
            for line in text.splitlines():
                if line.lower().startswith("senha:"):
                    return line.split(":", 1)[1].strip()
        time.sleep(0.5)
    raise AssertionError(f"outbox sem senha para {email} ({path})")


GERENTE_T13 = {
    "cpf": "55667788990",
    "nome": "Gumercindo",
    "email": "ger5@bantads.com.br",
    "telefone": "41988880005",
    "senha": "tads",
}

GERENTE_T13B = {
    "cpf": "66778899001",
    "nome": "Gundisalvo",
    "email": "ger6@bantads.com.br",
    "telefone": "41988880006",
    "senha": "tads",
}

GERENTE_T13_DUP = {
    "cpf": "77889900112",
    "nome": "Duplicado",
    "email": "ger1@bantads.com.br",
    "telefone": "41988880007",
    "senha": "tads",
}

CONTA_COANDRYA = "7617"
CONTA_CATHARYNA = "1291"


def poll_conta_gerente(numero: str, cpf_gerente: str, token: str) -> dict:
    time.sleep(2)
    body: dict = {}
    for _ in range(4):
        resp = requests.get(URL + f"/contas/{numero}", headers=auth_headers(token), timeout=10)
        assert resp.status_code == 200, resp.text
        body = resp.json()
        if body.get("cpfGerente") == cpf_gerente:
            return body
        time.sleep(5)
    raise AssertionError(f"conta {numero} não ficou com gerente {cpf_gerente}: {body}")

