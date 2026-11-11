"""Carrega exemplos JSON dos arquivos json-catalog/*.md (fonte única com a documentação)."""

from __future__ import annotations

import json
import re
from dataclasses import dataclass
from functools import lru_cache
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent.parent
CATALOG_DIR = REPO_ROOT / "json-catalog"

_JSON_BLOCK = re.compile(r"```json\s*\n(.*?)\n```", re.DOTALL)
_HEADING = re.compile(r"^### (.+)$", re.MULTILINE)
_BOLD_LABEL = re.compile(r"^\*\*(.+?)\*\*\s*$", re.MULTILINE)


@dataclass(frozen=True)
class CatalogExample:
    tx_file: str
    heading: str
    body: dict
    block_index: int


def _heading_before(content: str, json_start: int) -> str:
    prefix = content[:json_start]
    candidates: list[tuple[int, str]] = []
    for match in _BOLD_LABEL.finditer(prefix):
        label = match.group(1).strip().rstrip(":")
        candidates.append((match.start(), label))
    for match in _HEADING.finditer(prefix):
        candidates.append((match.start(), match.group(1).strip()))
    if not candidates:
        return f"block@{json_start}"
    candidates.sort(key=lambda item: item[0])
    return candidates[-1][1]


@lru_cache(maxsize=1)
def load_all_examples() -> tuple[CatalogExample, ...]:
    examples: list[CatalogExample] = []
    for path in sorted(CATALOG_DIR.glob("TX-*.md")):
        text = path.read_text(encoding="utf-8")
        for idx, match in enumerate(_JSON_BLOCK.finditer(text)):
            raw = match.group(1).strip()
            try:
                body = json.loads(raw)
            except json.JSONDecodeError:
                continue
            if not isinstance(body, dict):
                continue
            heading = _heading_before(text, match.start())
            examples.append(CatalogExample(path.name, heading, body, idx))
    return tuple(examples)


def find_example(tx_file: str, heading_contains: str) -> CatalogExample:
    tx_file = tx_file if tx_file.endswith(".md") else f"{tx_file}.md"
    for ex in load_all_examples():
        if ex.tx_file == tx_file and heading_contains in ex.heading:
            return ex
    raise KeyError(f"Exemplo não encontrado: {tx_file} / {heading_contains!r}")


def example_body(tx_file: str, heading_contains: str) -> dict:
    return find_example(tx_file, heading_contains).body
