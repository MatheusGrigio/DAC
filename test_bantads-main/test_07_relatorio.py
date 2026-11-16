import bantads as b

CAMPOS = {"cpf", "nome", "email", "salario", "numeroConta", "saldo", "cpfGerente", "nomeGerente"}


# ------------------------------- R16 Relatório de clientes ------------------------------- #
def R16_relatorio_clientes_1():
    tok = b.token("gerente")
    r = b.get("/relatorios/clientes", token=tok)
    assert r.status_code == 202
    job_id = r.json()["jobId"]
    job = b.poll_job(job_id, tok)
    assert job["status"] == "CONCLUIDO"
    assert job["resultType"] == "inline"
    res = b.get(f"/jobs/{job_id}/result", token=tok)
    assert res.status_code == 200
    clientes = res.json()["clientes"]
    assert len(clientes) >= 5
    assert CAMPOS <= set(clientes[0].keys())          # traz todos os campos do relatório
    nomes = [c["nome"] for c in clientes]
    assert nomes == sorted(nomes, key=b.chave_ordenacao)  # ordenado por nome do cliente
