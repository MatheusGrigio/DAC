"""Funções de apoio dos testes — mantém os testes curtos e legíveis.

- Lê `config.json` (server + porta) para montar a URL base.
- Guarda dados entre chamadas em `cache.json` (o token, ids criados, etc.),
  como o front-end faria no LocalStorage.
- Envolve as chamadas HTTP (requests) para não repetir cabeçalho/URL.
"""
import json
import os
import time
import unicodedata
from decimal import Decimal

import requests

AQUI = os.path.dirname(os.path.abspath(__file__))
CONFIG = json.load(open(os.path.join(AQUI, "config.json"), encoding="utf-8"))
BASE = f"{CONFIG['server']}:{CONFIG['port']}"
CACHE_PATH = os.path.join(AQUI, "cache.json")
# (não documentado) de onde o modo automático lê a senha da aprovação, se ligado
SENHA_PATH = CONFIG.get("senha_recente_path") or os.path.normpath(
    os.path.join(AQUI, "..", "back", "senha_recente.txt"))
TIMEOUT = 15


# --------------------------------------------------------------------------- #
# cache.json — dados compartilhados entre os testes                            #
# --------------------------------------------------------------------------- #
def cache_load():
    if os.path.exists(CACHE_PATH):
        with open(CACHE_PATH, encoding="utf-8") as f:
            return json.load(f)
    return {}


def cache_save(dados):
    with open(CACHE_PATH, "w", encoding="utf-8") as f:
        json.dump(dados, f, ensure_ascii=False, indent=2)


def cache_get(chave, padrao=None):
    return cache_load().get(chave, padrao)


def cache_set(chave, valor):
    dados = cache_load()
    dados[chave] = valor
    cache_save(dados)


def exigir(chave):
    """Lê um dado que um teste anterior gravou no cache. Se não existir (o teste
    foi rodado isolado, fora da ordem), pula o teste com uma mensagem clara em
    vez de falhar."""
    valor = cache_get(chave)
    if valor is None:
        import pytest
        pytest.skip(f"requer '{chave}' no cache.json — rode a bateria completa (ex.: 'pytest')")
    return valor


# --------------------------------------------------------------------------- #
# Chamadas HTTP                                                                #
# --------------------------------------------------------------------------- #
def _headers(token):
    # o token JWT vai no header x-access-token (igual ao verifyJWT do servidor)
    return {"x-access-token": token} if token else {}


def get(caminho, token=None, **params):
    return requests.get(BASE + caminho, headers=_headers(token),
                        params=params or None, timeout=TIMEOUT)


def post(caminho, corpo=None, token=None):
    return requests.post(BASE + caminho, json=corpo, headers=_headers(token), timeout=TIMEOUT)


def put(caminho, corpo=None, token=None):
    return requests.put(BASE + caminho, json=corpo, headers=_headers(token), timeout=TIMEOUT)


def delete(caminho, token=None):
    return requests.delete(BASE + caminho, headers=_headers(token), timeout=TIMEOUT)


# --------------------------------------------------------------------------- #
# Login / token (guardado no cache.json)                                       #
# --------------------------------------------------------------------------- #
CREDENCIAIS = {
    "gerente": {"email": "ger1@bantads.com.br", "senha": "tads"},
    "cliente": {"email": "cli1@bantads.com.br", "senha": "tads"},
}


def token(perfil):
    """Token do perfil ('gerente' ou 'cliente'). Usa o cache; loga se faltar."""
    chave = f"token_{perfil}"
    tok = cache_get(chave)
    if tok:
        return tok
    tok = post("/login", CREDENCIAIS[perfil]).json()["token"]
    cache_set(chave, tok)
    return tok


def pedir_senha(mensagem):
    """Pausa a execução e lê a senha que o usuário digita no terminal REAL —
    funciona mesmo com o pytest capturando a saída (não precisa de `-s`). É usada
    para logar como cliente novo, cuja senha só chega 'por e-mail' (o backend a
    imprime no console e grava em back/senha_recente.txt). Sem terminal
    interativo (ex.: CI/Windows), o teste é pulado com pytest.skip."""
    try:
        with open("/dev/tty", "r+") as tty:
            tty.write("\n" + mensagem)
            tty.flush()
            resp = tty.readline()
        if resp == "":  # EOF — sem quem digite
            raise OSError
        return resp.strip()
    except OSError:
        import pytest
        pytest.skip("teste interativo: rode 'pytest' num terminal real para digitar a senha")


def obter_senha(chave, email, cpf):
    """Pausa e pede que o usuário digite a senha do cliente novo (gerada na
    aprovação R9 e enviada por e-mail). Cacheia a senha obtida (chave 'senha_<chave>').

    (Não documentado no README) Se `automatic_password` estiver true no config, lê a
    senha do arquivo em vez de pedir — permite rodar sem intervenção contra o backend
    de referência, que grava a senha em senha_recente.txt."""
    if CONFIG.get("automatic_password"):
        senha = _ler_senha_arquivo(email)
    else:
        senha = pedir_senha(
            f"Digite a senha do cliente {email} (cpf {cpf}) — a que chegou por e-mail: ")
    cache_set(f"senha_{chave}", senha)
    return senha


def _ler_senha_arquivo(email_esperado=None):
    """(Não documentado) Lê a senha do arquivo senha_recente.txt — modo automático."""
    import pytest
    try:
        with open(SENHA_PATH, encoding="utf-8") as f:
            conteudo = f.read()
    except OSError:
        pytest.skip(f"automatic_password ligado, mas não li {SENHA_PATH}")
    senhas = [l.split("senha: ", 1)[1].strip()
              for l in conteudo.splitlines() if l.startswith("senha: ")]
    if not senhas:
        pytest.skip(f"não encontrei uma linha 'senha:' em {SENHA_PATH}")
    if email_esperado and email_esperado not in conteudo:
        # o arquivo tem a senha de OUTRO cliente — o login não veio logo após a aprovação
        pytest.skip(f"senha_recente.txt não corresponde a {email_esperado} (ordem dos testes?)")
    return senhas[-1]


# --------------------------------------------------------------------------- #
# Jobs assíncronos (202) — consulta o status até concluir                       #
# --------------------------------------------------------------------------- #
def poll_job(job_id, token, timeout=None):
    """Consulta /jobs/{id}/status até o job sair de PENDENTE.

    Pensado para o backend REAL: a SAGA (RabbitMQ) pode demorar e NÃO se sabe
    quanto — por isso o limite é um TIMEOUT total (config `job_timeout`, padrão
    generoso em segundos), não um número fixo de tentativas. O intervalo entre
    consultas cresce (backoff, começa rápido e vai até 2s) para responder logo
    quando é rápido e não martelar o servidor quando demora.

    Retorna o job assim que ele conclui (CONCLUIDO ou FALHA). Se ficar em
    PENDENTE além do prazo, FALHA com mensagem clara (timeout != FALHA de negócio).
    """
    limite = timeout if timeout is not None else CONFIG.get("job_timeout", 60)
    inicio = time.monotonic()
    intervalo = 0.1
    while True:
        job = get(f"/jobs/{job_id}/status", token=token).json()
        if job.get("status") != "PENDENTE":
            return job
        if time.monotonic() - inicio >= limite:
            import pytest
            pytest.fail(f"job {job_id} não concluiu em {limite}s (ficou em PENDENTE)")
        time.sleep(intervalo)
        intervalo = min(intervalo * 1.5, 2.0)


def poll_ate(condicao, timeout=None, descricao="condição"):
    """Repete condicao() (com backoff) até retornar verdadeiro, ou estoura o timeout.

    Para o backend REAL: o lado de leitura do CQRS (saldo, listagem, relatório,
    histórico) é atualizado de forma ASSÍNCRONA — uma leitura logo após a escrita
    pode vir defasada. Em vez de asserir na hora, reconsulta-se até a condição
    valer (config `read_timeout`, padrão generoso). Contra o mock (síncrono),
    passa na 1ª tentativa. Falha claro no timeout (distingue defasagem de bug)."""
    limite = timeout if timeout is not None else CONFIG.get("read_timeout", 15)
    inicio = time.monotonic()
    intervalo = 0.1
    while True:
        if condicao():
            return
        if time.monotonic() - inicio >= limite:
            import pytest
            pytest.fail(f"{descricao}: não ocorreu em {limite}s (read model defasado?)")
        time.sleep(intervalo)
        intervalo = min(intervalo * 1.5, 2.0)


def poll_saldo(numero, token, esperado, timeout=None):
    """Reconsulta o saldo (lado query) até bater com `esperado`, tolerando a
    defasagem do CQRS. Use no lugar de asserir o saldo logo após uma operação."""
    esperado = dec(esperado)
    poll_ate(lambda: saldo_conta(numero, token) == esperado, timeout,
             f"saldo da conta {numero} chegar a {esperado}")


# --------------------------------------------------------------------------- #
# Utilidades                                                                   #
# --------------------------------------------------------------------------- #
def saldo_conta(numero, token):
    return dec(get(f"/contas/{numero}", token=token).json()["saldo"])


def dec(valor):
    return Decimal(str(valor))


def chave_ordenacao(nome):
    """Mesma collation pt-BR do back: sem acento e minúsculo."""
    n = unicodedata.normalize("NFKD", nome or "")
    return "".join(c for c in n if not unicodedata.combining(c)).casefold()


_seq = 0


def _unico():
    global _seq
    _seq += 1
    return (int(time.time() * 1000) + _seq) % 100000000000


def novo_cpf():
    """CPF de teste com 11 dígitos, único por execução (o back não valida DV)."""
    return f"{_unico():011d}"


def novo_email(prefixo="teste"):
    return f"{prefixo}{_unico()}@bantads.com.br"
