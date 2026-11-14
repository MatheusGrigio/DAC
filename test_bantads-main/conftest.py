import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import pytest

import bantads as b
import integridade


def pytest_report_header(config):
    """Mostra o código de verificação no cabeçalho de toda execução."""
    ok, problemas = integridade.conferir()
    linhas = [f"código de verificação da suíte: {integridade.codigo()}"]
    if ok:
        linhas.append("integridade: OK — suíte idêntica à publicada")
    else:
        linhas.append("integridade: *** SUÍTE ALTERADA ***")
        linhas += [f"  {p}" for p in problemas]
    return linhas


@pytest.fixture(scope="session", autouse=True)
def preparar_ambiente():
    """Antes de tudo: limpa o cache e recria o seed via /reboot (sempre)."""
    b.cache_save({})   # começa a sessão sem tokens antigos
    b.post("/reboot")  # força o estado limpo a cada execução (garante testes isolados também)
    yield
