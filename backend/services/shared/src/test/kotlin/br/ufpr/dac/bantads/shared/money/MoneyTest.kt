package br.ufpr.dac.bantads.shared.money

import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MoneyTest {
    @Test
    fun `parse accepts two decimal places`() {
        val parsed = Money.parse("800.00")
        assertEquals(0, parsed.compareTo(BigDecimal("800.00")))
        assertEquals(2, parsed.scale())
    }

    @Test
    fun `parse accepts zero`() {
        assertEquals("0.00", Money.format(Money.parse("0.00")))
    }

    @Test
    fun `parse rejects integer without cents`() {
        assertFailsWith<IllegalArgumentException> { Money.parse("800") }
    }

    @Test
    fun `parse rejects one decimal place`() {
        assertFailsWith<IllegalArgumentException> { Money.parse("800.0") }
    }

    @Test
    fun `parse rejects three decimal places`() {
        assertFailsWith<IllegalArgumentException> { Money.parse("800.000") }
    }

    @Test
    fun `parse rejects negative values`() {
        assertFailsWith<IllegalArgumentException> { Money.parse("-1.00") }
    }

    @Test
    fun `parse rejects blank and non numeric`() {
        assertFailsWith<IllegalArgumentException> { Money.parse("") }
        assertFailsWith<IllegalArgumentException> { Money.parse("abc") }
        assertFailsWith<IllegalArgumentException> { Money.parse(" 800.00") }
    }

    @Test
    fun `format always emits two decimal places`() {
        assertEquals("800.00", Money.format(BigDecimal("800")))
        assertEquals("800.10", Money.format(BigDecimal("800.1")))
        assertEquals("150000.00", Money.format(BigDecimal("150000")))
    }

    @Test
    fun `format never uses scientific notation`() {
        assertEquals("0.01", Money.format(BigDecimal("0.01")))
    }

    @Test
    fun `add and subtract keep two decimal places`() {
        val a = Money.parse("810.00")
        val b = Money.parse("10.00")
        assertEquals("820.00", Money.format(Money.add(a, b)))
        assertEquals("800.00", Money.format(Money.subtract(a, b)))
    }

    @Test
    fun `isPositive and gte`() {
        assertTrue(Money.isPositive(Money.parse("0.01")))
        assertFalse(Money.isPositive(Money.parse("0.00")))
        assertTrue(Money.gte(Money.parse("800.00"), Money.parse("800.00")))
        assertFalse(Money.gte(Money.parse("799.99"), Money.parse("800.00")))
    }
}
