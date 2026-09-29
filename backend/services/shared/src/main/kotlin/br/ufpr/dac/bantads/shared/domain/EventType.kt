package br.ufpr.dac.bantads.shared.domain

import com.fasterxml.jackson.annotation.JsonValue

enum class EventType(
    @get:JsonValue val tipo: String,
) {
    CRIADO("Criado"),
    SAQUE("Saque"),
    DEPOSITO("Depósito"),
    TRANSFERENCIA_ORIGEM("TransferênciaOrigem"),
    TRANSFERENCIA_DESTINO("TransferênciaDestino"),
    GERENTE_ALTERADO("GerenteAlterado"),
}
