package br.ufpr.dac.bantads.shared.amqp

import com.fasterxml.jackson.annotation.JsonValue

enum class ReplyStatus(
    @get:JsonValue val wire: String,
) {
    SUCESSO("SUCESSO"),
    FALHA("FALHA"),
}
