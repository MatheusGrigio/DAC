package br.ufpr.dac.bantads.auth.password

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RandomPasswordTest {
    @Test
    fun `generates 8 alphanumeric characters`() {
        val password = RandomPassword.generate()
        assertEquals(8, password.length)
        assertTrue(password.all { it.isLetterOrDigit() })
    }
}
