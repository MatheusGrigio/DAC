# Suíte de testes do BANTADS — como executar

Esta é uma suíte **pytest** que testa o **seu backend** do BANTADS via HTTP. Ela
se comporta como um cliente da sua API (só fala com o **API Gateway**) e verifica
os requisitos R1–R16, o fluxo assíncrono (202/jobs), autenticação, HATEOAS e as
regras de negócio.

Este código está licenciado como
[GNU GPL v3](https://github.com/razeranthom/test_bantads/blob/main/LICENSE).

> A suíte usada em 2025-2 e 2026-1 (arquivo único `test_dac_bantads.py`) está
> preservada na tag [`2026-1`](https://github.com/razeranthom/test_bantads/tree/2026-1).

---

## 1. Pré-requisitos

- **Python 3.10+** e **pip**.
- Seu **backend no ar** e acessível (Front/Back/BD/containers subidos).
- Baixar a suíte e instalar as dependências (pytest e requests):

```bash
git clone https://github.com/razeranthom/test_bantads.git
cd test_bantads
python3 -m venv .venv        # opcional, recomendado
source .venv/bin/activate    # (no Windows: .venv\Scripts\activate)
pip install -r requirements.txt
```

## 2. O que seu backend precisa oferecer

A suíte segue o **enunciado** da disciplina (`BANTADS-DS152-DAC.pdf`) e o
**contrato da API** (`bantads-openapi.yaml` — cole no editor.swagger.io para
navegar), ambos distribuídos junto com o material do trabalho. Em resumo, para a
bateria rodar, seu backend precisa:

- Estar **acessível** em `server:port` — o **API Gateway** (a suíte NÃO fala com
  os microsserviços diretamente).
- Implementar **`POST /reboot`**, que **recria os dados pré-cadastrados** (seção 4
  do enunciado) e retorna 200. **É a primeira chamada** da suíte — sem ele, nada roda.
- Ter os **dados de seed** com senha **`tads`**: clientes `cli1..cli5@bantads.com.br`,
  gerentes `ger1..ger4@bantads.com.br`, com as contas/saldos do enunciado.
- **`POST /login`** (e-mail/senha) devolvendo `{ auth, token, tipo, usuario }`; o
  token vai nas demais chamadas no header **`x-access-token`**.
- Operações assíncronas (R9, R13, R15, R16) devolvendo **202 + `jobId`**, com
  **`GET /jobs/{id}/status`** e **`GET /jobs/{id}/result`**.

## 3. Configuração (`config.json`)

Edite **`config.json`** apontando para o **seu** backend:

```json
{
  "server": "http://localhost",
  "port": 8000,
  "novos_clientes": ["cliente.novo1@bantads.com.br", "cliente.novo2@bantads.com.br"],
  "novos_gerentes": ["gerente.novo1@bantads.com.br", "gerente.novo2@bantads.com.br"],
  "job_timeout": 60,
  "read_timeout": 15
}
```

- **`server` + `port`** → endereço do seu API Gateway (`server` deve incluir `http://`).
- **`novos_clientes` / `novos_gerentes`** → e-mails que a suíte usa para **criar e
  testar** 2 clientes e 2 gerentes novos (os CPFs são gerados na hora; a senha dos
  gerentes novos é `tads`). Pode trocar pelos e-mails que quiser.
- **`job_timeout`** (padrão 60) → segundos máximos esperando uma operação
  assíncrona (202) concluir. **Aumente** se sua SAGA/RabbitMQ for mais lenta.
- **`read_timeout`** (padrão 15) → segundos máximos esperando o **read model (CQRS)**
  refletir uma escrita (saldo/listagem/relatório podem demorar a atualizar).

## 4. Executar

```bash
pytest            # roda a bateria inteira
pytest -v         # mostra teste a teste (recomendado)
```

Rodar um arquivo ou um teste específico:

```bash
pytest test_05_contas.py
pytest test_05_contas.py::R05_saque_saldo_insuficiente_2
```

A suíte roda em ordem (arquivos `test_00_*` … `test_11_*`): reboota, confere o
seed, faz login e vai exercitando os requisitos.

## 5. O passo interativo: senha do cliente novo

Ao aprovar um cliente (R9), o backend gera uma senha **aleatória enviada por
e-mail** — a suíte não tem como descobri-la sozinha. Ao chegar no login do cliente
novo, a suíte **pausa e pede a senha**: leia a senha **no e-mail que o backend
enviou** e **digite no console**.

Por isso, **rode a suíte num terminal real**. Sem terminal (ex.: CI/redirecionamento),
esses testes de login de cliente são **pulados** (skip).

## 6. Lendo os resultados

- **`PASSED`** — ok, o backend respondeu como esperado.
- **`FAILED`** — algo não bateu; a mensagem diz o quê (status errado, saldo que não
  fechou, timeout, etc.).
- **`SKIPPED`** — o teste foi **pulado** (não é falha). Ocorre, por exemplo, quando
  ele depende de um login/criação que não aconteceu (ex.: login de cliente novo
  rodado sem terminal).
- **Timeout:** se um job assíncrono ou uma leitura pós-escrita demorar além do
  configurado, o teste falha dizendo "não concluiu/ocorreu em Xs" — aumente
  `job_timeout` / `read_timeout` se o seu backend for legitimamente mais lento.

## 7. Integridade da suíte

A suíte **não pode ser alterada**. Toda execução do pytest imprime no cabeçalho um
**código de verificação**, calculado sobre todos os arquivos de código:

```
código de verificação da suíte: 15D966AFCEF1
integridade: OK — suíte idêntica à publicada
```

**Na apresentação, o professor confere esse código.** Se qualquer arquivo `.py` ou o
`pytest.ini` for modificado, o código muda e o cabeçalho passa a acusar:

```
integridade: *** SUÍTE ALTERADA ***
  ALTERADO  test_05_contas.py
```

O `config.json` **fica de fora** do cálculo — você precisa editá-lo para apontar
para o seu backend, e isso não afeta o código de verificação.

Para conferir por fora do pytest:

```bash
python integridade.py
```

O código válido para a versão publicada está em `HASHES.txt` e é conferido
automaticamente a cada commit deste repositório. Se o seu código não bate, o
caminho é refazer o clone — não editar o `HASHES.txt`.

## 8. Como a suíte funciona (para os curiosos)

- **`bantads.py`** — funções de apoio (HTTP, `token`, `poll_job`, `poll_saldo`/`poll_ate`).
- **`cache.json`** — guarda dados entre os testes (token, ids criados, senhas
  obtidas), como o front faria no LocalStorage. É reescrito a cada execução.
- **`pytest.ini`** — coleta funções `R*`, `HEALTH*`, `JOB*` (em vez do padrão
  `test_*`); por isso os testes se chamam `R05_...`, `HEALTH_...`, etc.
- **Assíncrono (202):** os testes esperam o job em `/jobs/{id}/status` com timeout
  (`job_timeout`) e backoff, tolerando um backend que demora.
- **Consistência eventual (CQRS):** saldos/listagens/relatórios lidos logo após uma
  escrita são **reconsultados até convergir** (não asseridos na hora), tolerando a
  defasagem do read model (`read_timeout`).
- **Nomes:** `R<nn>_<cenário>_<n>` — o número é o do requisito testado.
