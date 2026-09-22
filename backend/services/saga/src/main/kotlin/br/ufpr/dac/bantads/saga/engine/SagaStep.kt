package br.ufpr.dac.bantads.saga.engine

data class SagaStep(
    val tipo: String,
    val kind: StepKind,
    val queue: String? = null,
    val compensationTipo: String? = null,
    val compensationKind: StepKind? = null,
    val compensationQueue: String? = null,
    val skipIfTrue: String? = null,
)
