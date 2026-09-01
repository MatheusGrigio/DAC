package br.ufpr.dac.bantads.cliente.solicitacao

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SolicitacaoRulesTest {
    @Test
    fun `only PENDENTE can be processed`() {
        assertTrue(SolicitacaoRules.canProcess(StatusSolicitacao.PENDENTE))
        assertFalse(SolicitacaoRules.canProcess(StatusSolicitacao.APROVADA))
        assertFalse(SolicitacaoRules.canProcess(StatusSolicitacao.NAO_APROVADA))
    }
}
