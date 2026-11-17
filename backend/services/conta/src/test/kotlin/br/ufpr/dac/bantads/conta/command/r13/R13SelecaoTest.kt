package br.ufpr.dac.bantads.conta.command.r13

import br.ufpr.dac.bantads.conta.command.event.EventReplay
import br.ufpr.dac.bantads.conta.command.seed.SeedContas
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class R13SelecaoTest {
    @Test
    fun `seed fifth manager receives account 7617`() {
        val states =
            SeedContas.toStored().groupBy { it.objetoId }.map { (numero, events) ->
                EventReplay.apply(numero, events)
            }
        val ativos =
            setOf(
                SeedContas.GENIEVE,
                SeedContas.GODOPHREDO,
                SeedContas.GYANDULA,
                "40501740066",
            )
        val escolhida = R13Selecao.escolher(states, ativos)
        assertEquals("7617", escolhida?.numero)
        assertEquals(SeedContas.COANDRYA, escolhida?.cpfCliente)
        assertEquals(SeedContas.GODOPHREDO, escolhida?.cpfGerente)
    }

    @Test
    fun `does not transfer when max accounts is at most one`() {
        val states =
            SeedContas
                .toStored()
                .groupBy { it.objetoId }
                .map { (numero, events) ->
                    EventReplay.apply(numero, events)
                }.filter { it.cpfGerente == SeedContas.GYANDULA }
        assertNull(R13Selecao.escolher(states, setOf(SeedContas.GYANDULA, "40501740066")))
    }
}
