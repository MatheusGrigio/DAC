"""Asserções alinhadas a 00-JSON-CATALOG.md e json-catalog/."""

import re
from typing import Any

from catalog_from_md import example_body
from catalog_match import assert_json_matches_catalog

DINHEIRO = re.compile(r"^\d+\.\d{2}$")
ISO_LOCAL = re.compile(r"^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}$")


def erro_http(status: int, erro: str, mensagem: str) -> dict[str, Any]:
    return {"status": status, "erro": erro, "mensagem": mensagem}


def auth_401(message: str) -> dict[str, Any]:
    return {"auth": False, "message": message}


def assert_erro_body(body: dict, status: int, erro: str, mensagem: str) -> None:
    assert body == erro_http(status, erro, mensagem), body


def assert_erro_catalog(body: dict, tx_file: str, heading_contains: str) -> None:
    assert_catalog_json(tx_file, heading_contains, body)


def assert_auth_401(body: dict, message: str) -> None:
    assert body == auth_401(message), body


def assert_auth_401_catalog(body: dict, tx_file: str, heading_contains: str) -> None:
    assert_catalog_json(tx_file, heading_contains, body)


def assert_no_links(body: dict) -> None:
    assert "_links" not in body, body


def assert_job_accepted(
    resp,
    tx_file: str = "TX-R9.md",
    heading: str = "Response 202 — Job aceito",
) -> dict:
    assert resp.status_code == 202, resp.text
    body = resp.json()
    assert_catalog_json(tx_file, heading, body)
    loc = resp.headers.get("Location")
    assert loc == f"/jobs/{body['jobId']}/status", loc
    return body


def assert_catalog_json(tx_file: str, heading_contains: str, actual: dict) -> None:
    """Corpo da resposta deve coincidir exatamente com o bloco JSON do json-catalog."""
    assert_json_matches_catalog(actual, example_body(tx_file, heading_contains))


def assert_catalog_response(resp, status: int, tx_file: str, heading_contains: str) -> dict:
    assert resp.status_code == status, resp.text
    body = resp.json()
    assert_catalog_json(tx_file, heading_contains, body)
    return body


def walk_dinheiro(node: Any, keys: set[str] | None = None) -> None:
    money_keys = keys or {"salario", "saldo", "valor", "saldoAbertura"}
    if isinstance(node, dict):
        for key, value in node.items():
            if key in money_keys and value is not None:
                assert isinstance(value, str), f"{key}={value!r}"
                assert DINHEIRO.fullmatch(value), f"{key}={value!r}"
            walk_dinheiro(value, money_keys)
    elif isinstance(node, list):
        for item in node:
            walk_dinheiro(item, money_keys)
