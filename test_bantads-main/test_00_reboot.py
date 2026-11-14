import bantads as b


# Primeira chamada da bateria: recria os dados do seed (estado conhecido).
def REBOOT_1():
    r = b.post("/reboot")
    assert r.status_code == 200
    assert r.json()["status"] == "ok"


# --- 3 chamadas em seguida para verificar se o reboot realmente aconteceu ---

def REBOOT_verifica_login_seed_2():
    # o usuário seed ger1 consegue logar (registro de Auth recriado)
    r = b.post("/login", {"email": "ger1@bantads.com.br", "senha": "tads"})
    assert r.status_code == 200
    assert r.json()["auth"] is True


def REBOOT_verifica_gerentes_seed_3():
    # o seed tem exatamente 4 gerentes ativos
    r = b.get("/gerentes", token=b.token("gerente"))
    assert r.status_code == 200
    assert len(r.json()["gerentes"]) == 4


def REBOOT_verifica_saldo_seed_4():
    # o saldo seed da conta 1291 (Catharyna) é 800.00 — prova que veio do seed
    r = b.get("/contas/1291", token=b.token("gerente"))
    assert r.status_code == 200
    assert r.json()["saldo"] == "800.00"
