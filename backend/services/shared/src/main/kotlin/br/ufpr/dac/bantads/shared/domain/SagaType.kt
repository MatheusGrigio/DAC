package br.ufpr.dac.bantads.shared.domain

import br.ufpr.dac.bantads.shared.amqp.CommandTypes
import com.fasterxml.jackson.annotation.JsonValue

enum class SagaType(
    @get:JsonValue val tipo: String,
) {
    APROVAR_CLIENTE(CommandTypes.APROVAR_CLIENTE),
    INSERIR_GERENTE(CommandTypes.INSERIR_GERENTE),
    REMOVER_GERENTE(CommandTypes.REMOVER_GERENTE),
    ECHO(CommandTypes.ECHO),
}
