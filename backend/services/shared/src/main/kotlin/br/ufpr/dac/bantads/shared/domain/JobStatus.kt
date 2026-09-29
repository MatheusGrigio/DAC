package br.ufpr.dac.bantads.shared.domain

import com.fasterxml.jackson.annotation.JsonValue

enum class JobStatus(
    @get:JsonValue val wire: String,
) {
    PENDENTE("PENDENTE"),
    CONCLUIDO("CONCLUIDO"),
    FALHA("FALHA"),
}
