package br.ufpr.dac.bantads.shared.domain

import com.fasterxml.jackson.annotation.JsonValue

enum class ResultType(
    @get:JsonValue val wire: String,
) {
    RESOURCE("resource"),
    INLINE("inline"),
}
