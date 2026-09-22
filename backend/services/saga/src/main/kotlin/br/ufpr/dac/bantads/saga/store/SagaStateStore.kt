package br.ufpr.dac.bantads.saga.store

import br.ufpr.dac.bantads.saga.engine.SagaState

interface SagaStateStore {
    fun find(sagaId: String): SagaState?

    fun save(state: SagaState)

    fun findInProgress(): List<SagaState>
}
