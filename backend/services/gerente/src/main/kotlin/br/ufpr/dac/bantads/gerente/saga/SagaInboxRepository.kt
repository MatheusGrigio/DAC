package br.ufpr.dac.bantads.gerente.saga

import org.springframework.data.jpa.repository.JpaRepository

interface SagaInboxRepository : JpaRepository<SagaInboxEntity, Long> {
    fun findBySagaIdAndTipo(
        sagaId: String,
        tipo: String,
    ): SagaInboxEntity?
}
