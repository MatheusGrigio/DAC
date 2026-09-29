package br.ufpr.dac.bantads.shared.money

import br.ufpr.dac.bantads.shared.json.BantadsJson
import com.fasterxml.jackson.module.kotlin.readValue
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

data class MoneySample(
    @get:MoneyJson val valor: BigDecimal,
)

class MoneyJsonTest {
    private val mapper = BantadsJson.mapper()

    @Test
    fun `money serializes as two-decimal JSON string`() {
        val json = mapper.writeValueAsString(MoneySample(Money.parse("800.00")))
        assertEquals("""{"valor":"800.00"}""", json)
        assertTrue(json.contains("\"800.00\""))
        assertTrue(!json.contains("800.0,"))
    }

    @Test
    fun `money deserializes from string`() {
        val parsed: MoneySample = mapper.readValue("""{"valor":"150000.00"}""")
        assertEquals(0, parsed.valor.compareTo(BigDecimal("150000.00")))
    }

    @Test
    fun `money rejects numeric JSON`() {
        assertFailsWith<Exception> {
            mapper.readValue<MoneySample>("""{"valor":800}""")
        }
    }

    @Test
    fun `money rejects one decimal place in JSON`() {
        assertFailsWith<Exception> {
            mapper.readValue<MoneySample>("""{"valor":"800.0"}""")
        }
    }
}
