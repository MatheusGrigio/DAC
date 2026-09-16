package br.ufpr.dac.bantads.conta.web

import br.ufpr.dac.bantads.shared.error.ErroBody

class ApiException(
    val body: ErroBody,
) : RuntimeException(body.mensagem)
