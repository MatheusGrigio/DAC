package br.ufpr.dac.bantads.saga.engine

enum class StepKind {
    LOCAL,
    TRANSACTIONAL,
    FIRE_AND_FORGET,
}
