package br.ufpr.dac.bantads.shared.amqp

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class MessageEnvelope(
    val sagaId: String? = null,
    val tipo: String,
    val timestamp: String,
    val payload: Map<String, Any?> = emptyMap(),
)
