"""TX-R11, TX-R12, TX-CAD-02, TX-R14, TX-R15 — catálogo."""

import requests

from catalog_assert import assert_catalog_json, assert_job_accepted
from helpers import (
    CPF_GADAMANTIO,
    CPF_GERENTE,
    CPF_GODOPHREDO,
    CPF_GYANDULA,
    URL,
    auth_headers,
    login,
    login_gerente,
    poll_job,
)


def test_tx_r11_lista_sem_filtro_cinco_clientes():
    token = login_gerente()
    resp = requests.get(URL + "/clientes", headers=auth_headers(token), timeout=15)
    assert resp.status_code == 200, resp.text
    assert_catalog_json("TX-R11.md", "Sem filtro, seed completo", resp.json())


def test_tx_r12_lista_quatro_gerentes():
    token = login_gerente()
    resp = requests.get(URL + "/gerentes", headers=auth_headers(token), timeout=10)
    assert resp.status_code == 200
    assert_catalog_json("TX-R12.md", "Response 200 — Seed", resp.json())


def test_tx_cad_02_gerente_200():
    token = login_gerente()
    resp = requests.get(URL + f"/gerentes/{CPF_GERENTE}", headers=auth_headers(token), timeout=10)
    assert resp.status_code == 200
    assert_catalog_json("TX-CAD-02.md", "Response 200 — Próprio gerente autenticado", resp.json())


def test_tx_cad_02_404():
    token = login_gerente()
    resp = requests.get(URL + "/gerentes/00000000000", headers=auth_headers(token), timeout=10)
    assert resp.status_code == 404
    assert_catalog_json("TX-CAD-02.md", "404 — CPF inexistente", resp.json())


def test_tx_r14_404_inexistente():
    token = login_gerente()
    resp = requests.put(
        URL + "/gerentes/00000000000",
        headers=auth_headers(token),
        json={"nome": "X", "telefone": "41999990000"},
        timeout=10,
    )
    assert resp.status_code == 404
    assert_catalog_json("TX-R14.md", "404 — CPF inexistente", resp.json())


def test_tx_r15_um_gerente_ativo_restante_self_delete_403():
    token = login("ger4@bantads.com.br")
    for cpf in (CPF_GERENTE, CPF_GODOPHREDO, CPF_GYANDULA):
        accepted = requests.delete(URL + f"/gerentes/{cpf}", headers=auth_headers(token), timeout=10)
        body = assert_job_accepted(accepted, "TX-R15.md", "Response 202 — Job aceito")
        job = poll_job(body["jobId"], token, timeout_s=90)
        assert job["status"] == "CONCLUIDO", job
    lista = requests.get(URL + "/gerentes", headers=auth_headers(token), timeout=10)
    assert lista.status_code == 200
    ativos = [g for g in lista.json()["gerentes"] if g["ativo"]]
    assert len(ativos) == 1
    assert ativos[0]["cpf"] == CPF_GADAMANTIO
    self_remove = requests.delete(
        URL + f"/gerentes/{CPF_GADAMANTIO}",
        headers=auth_headers(token),
        timeout=10,
    )
    assert self_remove.status_code == 403
    assert_catalog_json("TX-R15.md", "Response 403 — sem job", self_remove.json())
