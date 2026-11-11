"""Invoca HTTPie CLI e devolve status, headers e corpo JSON."""

from __future__ import annotations

import json
import re
import shutil
import subprocess
import tempfile
from pathlib import Path
from typing import Any

_STATUS = re.compile(r"^HTTP/\S+\s+(\d+)", re.MULTILINE)


class HttpieCliError(RuntimeError):
    pass


def require_httpie() -> str:
    path = shutil.which("http")
    if not path:
        raise SystemExit(
            "HTTPie CLI não encontrado no PATH. Instale: https://httpie.io/docs/cli/installation"
        )
    return path


def http_request(
    method: str,
    url: str,
    *,
    token: str | None = None,
    json_body: dict | list | None = None,
    query: dict[str, str] | None = None,
    timeout_s: int = 120,
) -> tuple[int, dict[str, str], Any]:
    """Executa `http` e parseia saída --print=Hhb (headers + body)."""
    require_httpie()
    cmd: list[str] = [
        "http",
        method.upper(),
        url,
        "--ignore-stdin",
        "--pretty=none",
        "--print=hb",
    ]
    if token:
        cmd.append(f"x-access-token:{token}")
    for key, value in (query or {}).items():
        cmd.append(f"{key}=={value}")

    tmp_json: Path | None = None
    if json_body is not None:
        flat = isinstance(json_body, dict) and all(
            not isinstance(v, (dict, list)) for v in json_body.values()
        )
        if flat:
            for key, value in json_body.items():
                cmd.append(f"{key}={value}" if value is not None else f"{key}=")
        else:
            tmp = tempfile.NamedTemporaryFile(
                mode="w",
                suffix=".json",
                delete=False,
                encoding="utf-8",
            )
            json.dump(json_body, tmp, ensure_ascii=False)
            tmp.close()
            tmp_json = Path(tmp.name)
            cmd.append(f"@{tmp_json}")

    try:
        run = subprocess.run(
            cmd,
            capture_output=True,
            text=True,
            timeout=timeout_s,
        )
    finally:
        if tmp_json is not None:
            tmp_json.unlink(missing_ok=True)
    if run.returncode != 0 and not run.stdout.strip():
        raise HttpieCliError(
            f"http {method} {url} falhou (exit {run.returncode}): {run.stderr.strip()}"
        )

    raw = run.stdout.replace("\r\n", "\n")
    match = _STATUS.search(raw)
    if not match:
        raise HttpieCliError(f"sem status HTTP na saída do httpie:\n{raw[:500]}")
    status = int(match.group(1))

    parts = raw.split("\n\n", 1)
    header_block = parts[0]
    body_block = parts[1] if len(parts) > 1 else ""
    headers: dict[str, str] = {}
    for line in header_block.splitlines():
        if ":" in line and not line.startswith("HTTP/"):
            name, _, value = line.partition(":")
            headers[name.strip().lower()] = value.strip()

    body: Any = {}
    text = body_block.strip()
    if text:
        if text.startswith("{") or text.startswith("["):
            body = json.loads(text)
        else:
            body = text

    return status, headers, body
