package br.ufpr.dac.bantads.auth.saga

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.mapping.Document

@Document("saga_inbox")
@CompoundIndex(name = "saga_inbox_unique", def = "{'sagaId': 1, 'tipo': 1}", unique = true)
data class SagaInbox(
    @Id val id: String? = null,
    val sagaId: String,
    val tipo: String,
    val replyJson: String,
)
