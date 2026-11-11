"""Comparação exata do JSON da API com o json-catalog (wildcards só onde o contrato é dinâmico)."""

from __future__ import annotations

import re
from copy import deepcopy
from typing import Any

from helpers import URL

DINHEIRO = re.compile(r"^\d+\.\d{2}$")
ISO_LOCAL = re.compile(r"^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}$")
ISO_DATE = re.compile(r"^\d{4}-\d{2}-\d{2}$")
CPF11 = re.compile(r"^\d{11}$")
MENSAGEM_R15 = re.compile(
    r"^Gerente removido; \d+ contas transferidas para .+$",
)

_UUID = re.compile(
    r"^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$",
    re.I,
)


def _inject_wildcards(node: Any, key: str | None = None) -> Any:
    if isinstance(node, dict):
        return {k: _inject_wildcards(v, k) for k, v in node.items()}
    if isinstance(node, list):
        return [_inject_wildcards(item, key) for item in node]
    if key == "quantidadeClientes":
        return "$NULL_OR_INT"
    if key in ("saldo", "saldoAbertura") and isinstance(node, str):
        return "$MONEY"
    if not isinstance(node, str):
        return node
    if key == "token":
        return "$JWT"
    if key == "jobId":
        return "$UUID"
    if key == "dataHoraProcessamento" and node is not None:
        return "$ISO_LOCAL"
    if key == "dataHora":
        return "$ISO_LOCAL"
    if key in ("dataInicio", "dataFim"):
        return "$ISO_DATE"
    if key == "nomeGerente":
        return "$NONEMPTY"
    if key in ("resourceId", "cpfGerente"):
        return "$CPF"
    if key == "numeroConta":
        return "$ACCOUNT4"
    if key == "mensagem" and isinstance(node, str) and node.startswith("Gerente removido"):
        return "$MENSAGEM_R15"
    if node.startswith("eyJ") or "..." in node:
        return "$JWT"
    if node.startswith("http://localhost:3000"):
        return node.replace("http://localhost:3000", URL)
    return node


def prepare_expected_from_catalog(body: dict) -> dict:
    return _inject_wildcards(deepcopy(body))


def _wildcard_ok(marker: str, actual: Any) -> bool:
    if marker == "$JWT":
        return isinstance(actual, str) and len(actual) > 10
    if marker == "$UUID":
        return isinstance(actual, str) and bool(_UUID.match(actual))
    if marker == "$ISO_LOCAL":
        return actual is None or (isinstance(actual, str) and bool(ISO_LOCAL.match(actual)))
    if marker == "$MONEY":
        return isinstance(actual, str) and bool(DINHEIRO.match(actual))
    if marker == "$ISO_DATE":
        return isinstance(actual, str) and bool(ISO_DATE.match(actual))
    if marker == "$NONEMPTY":
        return isinstance(actual, str) and len(actual) > 0
    if marker == "$CPF":
        return isinstance(actual, str) and bool(CPF11.match(actual))
    if marker == "$ACCOUNT4":
        return isinstance(actual, str) and len(actual) == 4 and actual.isdigit()
    if marker == "$INT":
        return isinstance(actual, int) and actual >= 0
    if marker == "$NULL_OR_INT":
        return actual is None or (isinstance(actual, int) and actual >= 0)
    if marker == "$MENSAGEM_R15":
        return isinstance(actual, str) and bool(MENSAGEM_R15.match(actual))
    return False


def _diff(path: str, expected: Any, actual: Any) -> list[str]:
    errors: list[str] = []
    if isinstance(expected, str) and expected.startswith("$"):
        if not _wildcard_ok(expected, actual):
            errors.append(f"{path}: esperado {expected}, recebido {actual!r}")
        return errors
    if type(expected) is not type(actual):
        errors.append(f"{path}: tipo {type(actual).__name__} != {type(expected).__name__}")
        return errors
    if isinstance(expected, dict):
        exp_keys = set(expected.keys())
        act_keys = set(actual.keys())
        if exp_keys != act_keys:
            missing = exp_keys - act_keys
            extra = act_keys - exp_keys
            if missing:
                errors.append(f"{path}: faltam chaves {sorted(missing)}")
            if extra:
                errors.append(f"{path}: chaves extras {sorted(extra)}")
        for k in sorted(exp_keys & act_keys):
            errors.extend(_diff(f"{path}.{k}" if path else k, expected[k], actual[k]))
        return errors
    if isinstance(expected, list):
        if len(expected) != len(actual):
            errors.append(f"{path}: lista len {len(actual)} != {len(expected)}")
            return errors
        for i, (e, a) in enumerate(zip(expected, actual, strict=True)):
            errors.extend(_diff(f"{path}[{i}]", e, a))
        return errors
    if expected != actual:
        errors.append(f"{path}: {actual!r} != {expected!r}")
    return errors


def assert_json_matches_catalog(actual: dict, catalog_body: dict) -> None:
    """Exige o mesmo conjunto de chaves e valores do exemplo do catálogo (token/jobId/datas dinâmicos)."""
    expected = prepare_expected_from_catalog(catalog_body)
    errors = _diff("body", expected, actual)
    if errors:
        joined = "\n".join(errors[:25])
        if len(errors) > 25:
            joined += f"\n... +{len(errors) - 25} diferenças"
        raise AssertionError(f"JSON diverge do json-catalog:\n{joined}")
