package br.ufpr.dac.bantads.shared.amqp

import kotlin.test.Test
import kotlin.test.assertEquals

class XDeathCountsTest {
    @Test
    fun `conta apenas a fila principal`() {
        val entries =
            listOf(
                mapOf("queue" to "ms.auth.cmd", "count" to 2),
                mapOf("queue" to "ms.auth.cmd.wait", "count" to 2),
            )
        assertEquals(2L, XDeathCounts.count(entries, "ms.auth.cmd"))
        assertEquals(0L, XDeathCounts.count(null, "ms.auth.cmd"))
    }
}
