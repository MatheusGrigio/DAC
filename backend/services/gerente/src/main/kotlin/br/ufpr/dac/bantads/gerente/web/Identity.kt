package br.ufpr.dac.bantads.gerente.web

import br.ufpr.dac.bantads.shared.domain.Perfil
import br.ufpr.dac.bantads.shared.error.ErroBody

object Identity {
    fun requireGerente(tipo: String) {
        if (tipo != Perfil.GERENTE.wire) {
            throw ApiException(ErroBody.forbidden("Acesso negado"))
        }
    }
}
