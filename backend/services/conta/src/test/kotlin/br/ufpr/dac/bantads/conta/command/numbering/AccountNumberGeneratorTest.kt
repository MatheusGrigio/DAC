package br.ufpr.dac.bantads.conta.command.numbering

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.random.Random

class AccountNumberGeneratorTest {
    @Test
    fun `twenty numbers are four digits and not the cpf prefix`() {
        val cpf = "12912861012"
        val seen = mutableSetOf<String>()
        repeat(20) { i ->
            val numero =
                AccountNumberGenerator.generate(cpf, { it in seen }, Random(i.toLong()))
            assertEquals(4, numero.length)
            assertTrue(numero.all { it.isDigit() })
            assertNotEquals("1291", numero)
            seen += numero
        }
        assertEquals(20, seen.size)
    }
}
