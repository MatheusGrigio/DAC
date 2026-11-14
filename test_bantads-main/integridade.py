"""Verificação de integridade da suíte.

Calcula um hash SHA-256 de cada arquivo de código da suíte e um "código de
verificação" agregado, que é impresso no cabeçalho de toda execução do pytest.

O arquivo config.json fica de fora de propósito: o aluno precisa editá-lo para
apontar para o backend dele.

Uso:
    python integridade.py            # confere e mostra o código
    python integridade.py --gerar    # regrava HASHES.txt (mantenedor)
"""

import hashlib
import pathlib
import sys

RAIZ = pathlib.Path(__file__).resolve().parent
MANIFESTO = RAIZ / "HASHES.txt"

# O que entra no hash: o código que decide se um teste passa ou falha.
PADROES = ("*.py", "pytest.ini")
IGNORAR = {".git", ".venv", "venv", "__pycache__", ".pytest_cache"}


def _raizes_venv():
    """Raízes de virtualenv sob RAIZ: qualquer diretório com um pyvenv.cfg.

    Pega venvs de QUALQUER nome (venv, venv_old, env, .venv311, ...), não só
    os nomes fixos de IGNORAR. Sem isso, um clone cujo virtualenv tenha nome
    diferente acusaria falso "INTEGRIDADE COMPROMETIDA" (todos os .py do venv
    entrariam como EXTRA).
    """
    return {cfg.parent for cfg in RAIZ.rglob("pyvenv.cfg")}


def arquivos():
    """Arquivos da suíte que entram no hash, em ordem estável."""
    venvs = _raizes_venv()
    achados = set()
    for padrao in PADROES:
        for f in RAIZ.rglob(padrao):
            partes = f.relative_to(RAIZ).parts
            if any(p in IGNORAR for p in partes):
                continue
            if any(v in f.parents for v in venvs):
                continue
            achados.add(f)
    return sorted(achados, key=lambda f: f.relative_to(RAIZ).as_posix())


def _normaliza(dados):
    """CRLF -> LF: o código não pode mudar por causa do SO do clone."""
    return dados.replace(b"\r\n", b"\n")


def hash_arquivo(f):
    return hashlib.sha256(_normaliza(f.read_bytes())).hexdigest()


def codigo():
    """Código de verificação agregado de toda a suíte."""
    h = hashlib.sha256()
    for f in arquivos():
        h.update(f.relative_to(RAIZ).as_posix().encode())
        h.update(b"\0")
        h.update(hash_arquivo(f).encode())
        h.update(b"\n")
    return h.hexdigest()[:12].upper()


def _manifesto_texto():
    linhas = [
        "# Integridade da suíte de testes do BANTADS.",
        "# Gerado por: python integridade.py --gerar",
        "# Confira com: python integridade.py",
        "",
    ]
    for f in arquivos():
        linhas.append(f"{hash_arquivo(f)}  {f.relative_to(RAIZ).as_posix()}")
    linhas += ["", f"CODIGO {codigo()}", ""]
    return "\n".join(linhas)


def ler_manifesto():
    """Lê HASHES.txt -> ({caminho: hash}, codigo). ({}, None) se não existir."""
    if not MANIFESTO.exists():
        return {}, None
    esperados, cod = {}, None
    for linha in MANIFESTO.read_text(encoding="utf-8").splitlines():
        linha = linha.strip()
        if not linha or linha.startswith("#"):
            continue
        if linha.startswith("CODIGO "):
            cod = linha.split(None, 1)[1].strip()
        elif "  " in linha:
            h, caminho = linha.split("  ", 1)
            esperados[caminho.strip()] = h.strip()
    return esperados, cod


def conferir():
    """Compara o disco com o manifesto -> (ok, lista de problemas)."""
    esperados, cod_esperado = ler_manifesto()
    if not esperados:
        return False, ["HASHES.txt não encontrado ou vazio"]

    problemas = []
    atuais = {f.relative_to(RAIZ).as_posix(): hash_arquivo(f) for f in arquivos()}

    for caminho, h in sorted(esperados.items()):
        if caminho not in atuais:
            problemas.append(f"FALTANDO  {caminho}")
        elif atuais[caminho] != h:
            problemas.append(f"ALTERADO  {caminho}")
    for caminho in sorted(atuais):
        if caminho not in esperados:
            problemas.append(f"EXTRA     {caminho}")

    if cod_esperado and codigo() != cod_esperado:
        problemas.append(f"CODIGO    esperado {cod_esperado}, obtido {codigo()}")

    return not problemas, problemas


def main():
    if "--gerar" in sys.argv:
        MANIFESTO.write_text(_manifesto_texto(), encoding="utf-8")
        print(f"HASHES.txt regravado — código de verificação: {codigo()}")
        return 0

    ok, problemas = conferir()
    print(f"Arquivos conferidos: {len(arquivos())}")
    print(f"Código de verificação: {codigo()}")
    if ok:
        print("Integridade OK — a suíte está idêntica à publicada.")
        return 0
    print("\nINTEGRIDADE COMPROMETIDA:")
    for p in problemas:
        print(f"  {p}")
    return 1


if __name__ == "__main__":
    sys.exit(main())
