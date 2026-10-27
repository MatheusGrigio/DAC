package br.ufpr.dac.bantads.conta.command.publish

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface EventOutboxRepository : JpaRepository<EventOutboxEntity, UUID> {
    fun findTop50ByPublishedFalseOrderByCreatedAtAsc(): List<EventOutboxEntity>
}
