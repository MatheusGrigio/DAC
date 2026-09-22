package br.ufpr.dac.bantads.saga.engine

data class SagaState(
    val sagaId: String,
    val tipo: String,
    val etapaAtual: Int,
    val status: String,
    val payload: Map<String, Any?> = emptyMap(),
    val timestamp: String,
    val waitingTipo: String? = null,
    val timeoutAtEpochMs: Long? = null,
    val succeededTipos: List<String> = emptyList(),
    val compensacoes: Int = 0,
)
