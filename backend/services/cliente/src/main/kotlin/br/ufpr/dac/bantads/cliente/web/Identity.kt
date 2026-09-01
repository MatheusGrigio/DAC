package br.ufpr.dac.bantads.cliente.web

import br.ufpr.dac.bantads.shared.domain.Perfil
import br.ufpr.dac.bantads.shared.error.ErroBody

object Identity {
    fun requireGerente(tipo: String) {
        if (tipo != Perfil.GERENTE.wire) {
            throw ApiException(ErroBody.forbidden("Acesso negado"))
        }
    }

    fun requireGerenteOrSelf(
        tipo: String,
        userCpf: String,
        resourceCpf: String,
    ) {
        if (tipo == Perfil.GERENTE.wire) return
        if (tipo == Perfil.CLIENTE.wire && userCpf == resourceCpf) return
        throw ApiException(ErroBody.forbidden("Acesso negado"))
    }

    fun requireAuthenticated(tipo: String) {
        if (tipo != Perfil.GERENTE.wire && tipo != Perfil.CLIENTE.wire) {
            throw ApiException(ErroBody.forbidden("Acesso negado"))
        }
    }
}
