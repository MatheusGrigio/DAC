package br.ufpr.dac.bantads.shared.domain

import com.fasterxml.jackson.annotation.JsonValue

enum class Perfil(
    @get:JsonValue val wire: String,
) {
    CLIENTE("CLIENTE"),
    GERENTE("GERENTE"),
}
