import requests

from catalog_assert import assert_catalog_json
from helpers import HEADERS, URL


def test_t00_reboot():
    first = requests.post(URL + "/reboot", headers=HEADERS, timeout=90)
    assert first.status_code == 200
    assert_catalog_json("TX-INFRA-02.md", "Response 200 — Seed recriado", first.json())

    second = requests.post(URL + "/reboot", headers=HEADERS, timeout=90)
    assert second.status_code == 200
    assert_catalog_json("TX-INFRA-02.md", "Response 200 — Seed recriado", second.json())


def test_t00b_health():
    resp = requests.get(URL + "/health", headers=HEADERS, timeout=10)
    assert resp.status_code == 200
    assert_catalog_json("TX-INFRA-01.md", "Response 200 — Gateway no ar", resp.json())
