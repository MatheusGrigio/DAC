import requests

from helpers import HEADERS, URL


def test_t00_reboot():
    expected = {"status": "ok", "clientes": 5, "gerentes": 4, "contas": 5}
    first = requests.post(URL + "/reboot", headers=HEADERS, timeout=90)
    assert first.status_code == 200
    assert first.json() == expected
    assert "_links" not in first.json()

    second = requests.post(URL + "/reboot", headers=HEADERS, timeout=90)
    assert second.status_code == 200
    assert second.json() == expected
    assert second.json() == first.json()


def test_t00b_health():
    resp = requests.get(URL + "/health", headers=HEADERS, timeout=10)
    assert resp.status_code == 200
    assert resp.json() == {"status": "UP"}
    assert "_links" not in resp.json()
