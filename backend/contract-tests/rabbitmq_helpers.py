"""RabbitMQ Management API — filas SAGA (transacoes/00-ASSINCRONAS.md)."""

import os
import time

import requests

RABBIT_API = os.getenv("RABBIT_MANAGEMENT_URL", "http://localhost:15672/api").rstrip("/")
RABBIT_USER = os.getenv("RABBIT_USER", "bantads")
RABBIT_PASSWORD = os.getenv("RABBIT_PASSWORD", "change-me")

SAGA_QUEUES = [
    "saga.cmd",
    "orquestrador.reply",
    "ms.cliente.cmd",
    "ms.conta.cmd",
    "ms.gerente.cmd",
    "ms.auth.cmd",
    "ms.email.cmd",
    "ms.conta.events",
]

DLQ_QUEUES = [
    "ms.cliente.cmd.dlq",
    "ms.conta.cmd.dlq",
    "ms.gerente.cmd.dlq",
    "ms.auth.cmd.dlq",
    "ms.conta.events.dlq",
]


def _auth() -> tuple[str, str]:
    return RABBIT_USER, RABBIT_PASSWORD


def management_ready() -> bool:
    try:
        r = requests.get(f"{RABBIT_API}/overview", auth=_auth(), timeout=3)
        return r.status_code == 200
    except requests.RequestException:
        return False


def queue_stats(name: str) -> dict:
    r = requests.get(f"{RABBIT_API}/queues/%2F/{name}", auth=_auth(), timeout=10)
    r.raise_for_status()
    return r.json()


def messages_ready(name: str) -> int:
    return int(queue_stats(name).get("messages_ready", 0))


def wait_queues_idle(
    names: list[str],
    max_ready: int = 0,
    timeout_s: float = 30,
    poll_s: float = 0.5,
) -> None:
    deadline = time.time() + timeout_s
    last: dict[str, int] = {}
    while time.time() < deadline:
        last = {n: messages_ready(n) for n in names}
        if all(v <= max_ready for v in last.values()):
            return
        time.sleep(poll_s)
    raise AssertionError(f"filas não ficaram ociosas (ready<={max_ready}): {last}")


def assert_dlq_clean(names: list[str] | None = None) -> None:
    for name in names or DLQ_QUEUES:
        ready = messages_ready(name)
        assert ready == 0, f"DLQ {name} tem {ready} mensagem(ns) pronta(s)"
