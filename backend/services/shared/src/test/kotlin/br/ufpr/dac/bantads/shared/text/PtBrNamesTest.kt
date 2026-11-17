package br.ufpr.dac.bantads.shared.text

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PtBrNamesTest {
    @Test
    fun `seed clientes follow pt-BR base order`() {
        val shuffled = listOf("Cutardo", "Catharyna", "Coândrya", "Cleuddônio", "Catianna")
        assertEquals(
            listOf("Catharyna", "Catianna", "Cleuddônio", "Coândrya", "Cutardo"),
            PtBrNames.sort(shuffled),
        )
    }

    @Test
    fun `seed gerentes follow pt-BR base order`() {
        val shuffled = listOf("Gyândula", "Godophredo", "Geniéve", "Gadamântio")
        assertEquals(
            listOf("Gadamântio", "Geniéve", "Godophredo", "Gyândula"),
            PtBrNames.sort(shuffled),
        )
    }

    @Test
    fun `accents equal the base letter`() {
        assertEquals(0, PtBrNames.compare("Catianna", "Catiánna"))
        assertTrue(PtBrNames.compare("Coândrya", "Cutardo") < 0)
    }
}
