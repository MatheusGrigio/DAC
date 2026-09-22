package br.ufpr.dac.bantads.saga.engine

interface CacheInvalidator {
    fun deleteCliente(cpf: String?)

    fun deleteGerente(cpf: String?)

    fun deleteSessions(cpf: String?)
}
