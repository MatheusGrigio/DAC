package br.ufpr.dac.bantads.conta.command.event

import br.ufpr.dac.bantads.shared.money.Money
import java.math.BigDecimal

object EventReplay {
    fun apply(
        numero: String,
        events: List<StoredEvent>,
    ): AccountState {
        var state = AccountState(numero = numero)
        events.sortedBy { it.versao }.forEach { event ->
            state = applyOne(state, event)
        }
        return state
    }

    private fun applyOne(
        state: AccountState,
        event: StoredEvent,
    ): AccountState {
        val payload = event.payload
        return when (event.tipo) {
            EventTypes.CRIADO ->
                state.copy(
                    existe = true,
                    cpfCliente = texto(payload, "cpfCliente"),
                    cpfGerente = texto(payload, "cpfGerente"),
                    saldo = dinheiro(payload, "saldoInicial") ?: Money.parse("0.00"),
                    dataCriacao = texto(payload, "dataCriacao"),
                    versao = event.versao,
                )
            EventTypes.DEPOSITO ->
                state.copy(saldo = Money.add(state.saldo, valor(payload)), versao = event.versao)
            EventTypes.SAQUE, EventTypes.TRANSFERENCIA_ORIGEM ->
                state.copy(saldo = Money.subtract(state.saldo, valor(payload)), versao = event.versao)
            EventTypes.TRANSFERENCIA_DESTINO ->
                state.copy(saldo = Money.add(state.saldo, valor(payload)), versao = event.versao)
            EventTypes.GERENTE_ALTERADO ->
                state.copy(cpfGerente = texto(payload, "cpfGerente"), versao = event.versao)
            EventTypes.REMOVIDO ->
                state.copy(existe = false, versao = event.versao)
            else -> state.copy(versao = event.versao)
        }
    }

    fun valor(payload: Map<String, Any?>): BigDecimal = dinheiro(payload, "valor") ?: error("payload.valor ausente")

    fun dinheiro(
        payload: Map<String, Any?>,
        key: String,
    ): BigDecimal? {
        val raw = payload[key] ?: return null
        val bruto =
            when (raw) {
                is String -> raw
                else -> Money.format(BigDecimal(raw.toString()))
            }
        return Money.parse(bruto)
    }

    fun texto(
        payload: Map<String, Any?>,
        key: String,
    ): String? = payload[key]?.toString()?.trim()?.takeIf { it.isNotEmpty() }
}
