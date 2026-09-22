package br.ufpr.dac.bantads.saga.engine

interface CompensationGuard {
    fun tryAcquire(
        sagaId: String,
        etapa: Int,
    ): Boolean
}
