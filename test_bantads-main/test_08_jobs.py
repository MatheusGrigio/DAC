import bantads as b


# ------------------------------- /jobs/{id}/status ------------------------------- #
def JOB_status_1():
    tok = b.token("gerente")
    # dispara um relatório (operação assíncrona) para ter um job
    job_id = b.get("/relatorios/clientes", token=tok).json()["jobId"]
    r = b.get(f"/jobs/{job_id}/status", token=tok)
    assert r.status_code == 200
    assert r.json()["jobId"] == job_id
    assert r.json()["status"] in ("PENDENTE", "CONCLUIDO")
    b.cache_set("job_inline", job_id)


# ------------------------------- /jobs/{id}/result ------------------------------- #
def JOB_result_2():
    tok = b.token("gerente")
    job_id = b.exigir("job_inline")
    b.poll_job(job_id, tok)  # garante conclusão
    r = b.get(f"/jobs/{job_id}/result", token=tok)
    assert r.status_code == 200
    assert "clientes" in r.json()


def JOB_status_inexistente_3():
    r = b.get("/jobs/nao-existe-123/status", token=b.token("gerente"))
    assert r.status_code == 404
