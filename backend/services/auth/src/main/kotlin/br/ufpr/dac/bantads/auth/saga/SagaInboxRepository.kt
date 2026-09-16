package br.ufpr.dac.bantads.auth.saga

import org.springframework.data.mongodb.repository.MongoRepository

interface SagaInboxRepository : MongoRepository<SagaInbox, String> {
    fun findBySagaIdAndTipo(
        sagaId: String,
        tipo: String,
    ): SagaInbox?
}
