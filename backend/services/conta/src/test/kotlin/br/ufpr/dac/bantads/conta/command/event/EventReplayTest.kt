package br.ufpr.dac.bantads.conta.command.event

import br.ufpr.dac.bantads.conta.command.seed.SeedContas
import br.ufpr.dac.bantads.shared.money.Money
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EventReplayTest {
    @Test
    fun `replay Catharyna 1291 equals 800_00`() {
        val events = SeedContas.toStored().filter { it.objetoId == "1291" }
        val state = EventReplay.apply("1291", events)
        assertEquals(Money.parse("800.00"), state.saldo)
        assertEquals(SeedContas.CATHARYNA, state.cpfCliente)
        assertEquals(SeedContas.GENIEVE, state.cpfGerente)
        assertEquals(true, state.existe)
    }

    @Test
    fun `replay seed balances match enunciado`() {
        val states =
            SeedContas.toStored().groupBy { it.objetoId }.mapValues { (numero, events) ->
                EventReplay.apply(numero, events)
            }
        assertEquals(Money.parse("800.00"), states.getValue("1291").saldo)
        assertEquals(Money.parse("10000.00"), states.getValue("0950").saldo)
        assertEquals(Money.parse("200.00"), states.getValue("8573").saldo)
        assertEquals(Money.parse("150000.00"), states.getValue("5887").saldo)
        assertEquals(Money.parse("1500.00"), states.getValue("7617").saldo)
    }

    @Test
    fun `seed versions are unique per account`() {
        val keys = SeedContas.toStored().map { it.objetoId to it.versao }
        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun `seed event ids are deterministic`() {
        assertEquals(SeedContas.toStored().map { it.id }, SeedContas.toStored().map { it.id })
    }

    @Test
    fun `GerenteAlterado updates manager without touching saldo`() {
        val events =
            listOf(
                stored(
                    "1291",
                    EventTypes.CRIADO,
                    1,
                    mapOf("cpfCliente" to "1", "cpfGerente" to "g1", "saldoInicial" to "0.00"),
                ),
                stored("1291", EventTypes.DEPOSITO, 2, mapOf("valor" to "100.00")),
                stored("1291", EventTypes.GERENTE_ALTERADO, 3, mapOf("cpfGerente" to "g2")),
            )
        val state = EventReplay.apply("1291", events)
        assertEquals("g2", state.cpfGerente)
        assertEquals(Money.parse("100.00"), state.saldo)
        assertEquals(true, state.existe)
    }

    @Test
    fun `Removido marks account as gone for SAGA compensation`() {
        val events =
            listOf(
                stored(
                    "4321",
                    EventTypes.CRIADO,
                    1,
                    mapOf("cpfCliente" to "1", "cpfGerente" to "g1", "saldoInicial" to "0.00"),
                ),
                stored("4321", EventTypes.REMOVIDO, 2, emptyMap()),
            )
        val state = EventReplay.apply("4321", events)
        assertEquals(false, state.existe)
        assertEquals(2, state.versao)
    }

    private fun stored(
        numero: String,
        tipo: String,
        versao: Int,
        payload: Map<String, Any?>,
    ) = StoredEvent(
        id = java.util.UUID.fromString("00000000-0000-0000-0000-00000000000$versao"),
        objetoId = numero,
        tipo = tipo,
        payload = payload,
        versao = versao,
        timestamp = java.time.LocalDateTime.parse("2020-01-01T10:00:00"),
    )
}
