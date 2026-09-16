package br.ufpr.dac.bantads.conta.query.extrato

import br.ufpr.dac.bantads.conta.web.ApiException
import br.ufpr.dac.bantads.shared.money.Money
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ExtratoRegrasTest {
    @Test
    fun `defaults to last 30 days`() {
        val hoje = LocalDate.of(2026, 4, 30)
        val (inicio, fim) = ExtratoRegras.periodo(null, null, hoje)
        assertEquals(hoje.minusDays(30), inicio)
        assertEquals(hoje, fim)
    }

    @Test
    fun `fim before inicio is 422`() {
        val ex =
            assertThrows(ApiException::class.java) {
                ExtratoRegras.periodo(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 4, 30))
            }
        assertEquals(422, ex.body.status)
    }

    @Test
    fun `interval longer than 365 days is 422`() {
        val ex =
            assertThrows(ApiException::class.java) {
                ExtratoRegras.periodo(LocalDate.of(2020, 1, 1), LocalDate.of(2021, 1, 2), LocalDate.of(2026, 4, 30))
            }
        assertEquals(422, ex.body.status)
    }

    @Test
    fun `opening balance uses movements before period`() {
        val abertura =
            ExtratoRegras.saldoAbertura(
                "1291",
                listOf(
                    MovimentoDelta(MovimentacaoTipos.DEPOSITO, Money.parse("100.00")),
                    MovimentoDelta(MovimentacaoTipos.SAQUE, Money.parse("40.00")),
                ),
            )
        assertEquals(Money.parse("60.00"), abertura)
    }

    @Test
    fun `opening balance applies transfer as origin or destination`() {
        val saida =
            ExtratoRegras.saldoAbertura(
                "1291",
                listOf(
                    MovimentoDelta(MovimentacaoTipos.DEPOSITO, Money.parse("200.00")),
                    MovimentoDelta(
                        MovimentacaoTipos.TRANSFERENCIA,
                        Money.parse("50.00"),
                        origemNumero = "1291",
                        destinoNumero = "0950",
                    ),
                ),
            )
        assertEquals(Money.parse("150.00"), saida)
        val entrada =
            ExtratoRegras.saldoAbertura(
                "0950",
                listOf(
                    MovimentoDelta(
                        MovimentacaoTipos.TRANSFERENCIA,
                        Money.parse("50.00"),
                        origemNumero = "1291",
                        destinoNumero = "0950",
                    ),
                ),
            )
        assertEquals(Money.parse("50.00"), entrada)
    }
}
