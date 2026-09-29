package br.ufpr.dac.bantads.shared.amqp

data class ReplyEnvelope(
    val sagaId: String,
    val tipo: String,
    val timestamp: String,
    val status: ReplyStatus,
    val erro: String? = null,
    val payload: Map<String, Any?> = emptyMap(),
)
