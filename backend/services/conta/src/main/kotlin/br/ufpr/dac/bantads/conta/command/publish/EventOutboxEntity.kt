package br.ufpr.dac.bantads.conta.command.publish

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "event_outbox")
class EventOutboxEntity(
    @Id var id: UUID,
    @Column(nullable = false, columnDefinition = "TEXT") var body: String,
    @Column(nullable = false) var published: Boolean = false,
    @Column(name = "created_at", nullable = false) var createdAt: LocalDateTime,
)
