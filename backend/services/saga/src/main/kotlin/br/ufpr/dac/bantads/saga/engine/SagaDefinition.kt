package br.ufpr.dac.bantads.saga.engine

data class SagaDefinition(
    val tipo: String,
    val steps: List<SagaStep>,
)
