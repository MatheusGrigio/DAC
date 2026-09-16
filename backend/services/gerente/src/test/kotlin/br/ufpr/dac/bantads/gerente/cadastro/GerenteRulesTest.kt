package br.ufpr.dac.bantads.gerente.cadastro

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GerenteRulesTest {
    @Test
    fun `cannot inactivate the last active manager`() {
        assertFalse(GerenteRules.canInativar(targetAtivo = true, ativos = 1L))
        assertTrue(GerenteRules.canInativar(targetAtivo = true, ativos = 2L))
        assertFalse(GerenteRules.canInativar(targetAtivo = false, ativos = 3L))
    }
}
