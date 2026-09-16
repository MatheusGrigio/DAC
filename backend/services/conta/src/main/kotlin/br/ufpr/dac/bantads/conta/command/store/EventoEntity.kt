package br.ufpr.dac.bantads.conta.command.store

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "evento")
class EventoEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @Column(name = "objeto_id", nullable = false, length = 4) var objetoId: String = "",
    @Column(nullable = false, length = 40) var tipo: String = "",
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    var payload: Map<String, Any?> = emptyMap(),
    @Column(nullable = false) var versao: Int = 0,
    @Column(nullable = false) var timestamp: LocalDateTime = LocalDateTime.now(),
)
