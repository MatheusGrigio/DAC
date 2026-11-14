import bantads as b


def HEALTH_status_1():
    r = b.get("/health")
    assert r.status_code == 200
    assert r.json()["status"] == "UP"
