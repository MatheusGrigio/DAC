package br.ufpr.dac.bantads.cliente.saga

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "saga_inbox")
class SagaInboxEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
    @Column(name = "saga_id", nullable = false) var sagaId: String = "",
    @Column(nullable = false) var tipo: String = "",
    @Column(name = "reply_json", nullable = false, columnDefinition = "TEXT") var replyJson: String = "",
)
