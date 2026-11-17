"""TX-R9, TX-R13 + filas RabbitMQ (transacoes/00-ASSINCRONAS.md)."""

import time

import pytest
import requests

from catalog_assert import assert_catalog_json, assert_job_accepted
from helpers import (
    GERENTE_T13,
    GERENTE_T13_DUP,
    HEADERS,
    SOLICITACAO_T09,
    URL,
    auth_headers,
    login_gerente,
    poll_job,
    senha_outbox,
)
from rabbitmq_helpers import (
    DLQ_QUEUES,
    SAGA_QUEUES,
    assert_dlq_clean,
    management_ready,
    wait_queues_idle,
)


@pytest.fixture(scope="module")
def require_rabbit():
    if not management_ready():
        pytest.skip("RabbitMQ Management API indisponível (porta 15672)")


def test_tx_r9_a_saga_conclui_filas_ociosas(require_rabbit):
    created = requests.post(URL + "/solicitacoes", headers=HEADERS, json=SOLICITACAO_T09, timeout=10)
    assert created.status_code == 201, created.text
    assert_catalog_json("TX-R1.md", "Solicitação Beltrano (aceite T09)", created.json())
    token = login_gerente()
    accepted = requests.post(
        URL + f"/solicitacoes/{SOLICITACAO_T09['cpf']}/aprovacao",
        headers=auth_headers(token),
        timeout=10,
    )
    body = assert_job_accepted(accepted)
    job = poll_job(body["jobId"], token, timeout_s=60)
    assert_catalog_json("TX-R9.md", "Polling → GET /jobs/{jobId}/status → CONCLUIDO", job)
    senha_outbox(SOLICITACAO_T09["email"])
    time.sleep(1)
    wait_queues_idle(SAGA_QUEUES, max_ready=0, timeout_s=45)
    assert_dlq_clean()


def test_tx_r13_a_inserir_gerente_saga(require_rabbit):
    token = login_gerente()
    resp = requests.post(URL + "/gerentes", headers=auth_headers(token), json=GERENTE_T13, timeout=10)
    body = assert_job_accepted(resp, "TX-R13.md", "Response 202 — Job aceito")
    job = poll_job(body["jobId"], token, timeout_s=60)
    assert_catalog_json("TX-R13.md", "Polling → CONCLUIDO resource", job)
    wait_queues_idle(SAGA_QUEUES, max_ready=0, timeout_s=45)
    assert_dlq_clean(DLQ_QUEUES)


def test_tx_r13_b_falha_email_duplicado(require_rabbit):
    token = login_gerente()
    resp = requests.post(URL + "/gerentes", headers=auth_headers(token), json=GERENTE_T13_DUP, timeout=10)
    body = assert_job_accepted(resp, "TX-R13.md", "Response 202 — Job aceito")
    job = poll_job(body["jobId"], token, timeout_s=60)
    assert_catalog_json("TX-R13.md", "Job FALHA — E-mail duplicado", job)
    orphan = requests.get(
        URL + f"/gerentes/{GERENTE_T13_DUP['cpf']}",
        headers=auth_headers(token),
        timeout=10,
    )
    assert orphan.status_code == 404
    assert_catalog_json("TX-CAD-02.md", "404 — CPF inexistente", orphan.json())
