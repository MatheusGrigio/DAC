package br.ufpr.dac.bantads.conta.query.extrato

import br.ufpr.dac.bantads.conta.web.ApiException
import br.ufpr.dac.bantads.shared.error.ErroBody
import br.ufpr.dac.bantads.shared.money.Money
import java.math.BigDecimal
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class MovimentoDelta(
    val tipo: String,
    val valor: BigDecimal,
    val origemNumero: String? = null,
    val destinoNumero: String? = null,
)

object ExtratoRegras {
    const val MAX_DIAS = 365L
    const val DEFAULT_DIAS = 30L

    fun periodo(
        inicio: LocalDate?,
        fim: LocalDate?,
        hoje: LocalDate,
    ): Pair<LocalDate, LocalDate> {
        val fimEfetivo = fim ?: hoje
        val inicioEfetivo = inicio ?: fimEfetivo.minusDays(DEFAULT_DIAS)
        if (fimEfetivo.isBefore(inicioEfetivo)) {
            throw ApiException(ErroBody.unprocessable("Intervalo inválido: fim anterior ao início"))
        }
        if (ChronoUnit.DAYS.between(inicioEfetivo, fimEfetivo) > MAX_DIAS) {
            throw ApiException(ErroBody.unprocessable("Intervalo maior que 365 dias"))
        }
        return inicioEfetivo to fimEfetivo
    }

    fun saldoAbertura(
        numeroConta: String,
        movimentosAntes: List<MovimentoDelta>,
    ): BigDecimal {
        var saldo = Money.parse("0.00")
        movimentosAntes.forEach { movimento ->
            saldo =
                aplicar(
                    saldo,
                    movimento.tipo,
                    movimento.valor,
                    numeroConta,
                    movimento.origemNumero,
                    movimento.destinoNumero,
                )
        }
        return saldo
    }

    fun aplicar(
        saldo: BigDecimal,
        tipo: String,
        valor: BigDecimal,
        numeroConta: String,
        origemNumero: String?,
        destinoNumero: String?,
    ): BigDecimal =
        when (tipo) {
            MovimentacaoTipos.DEPOSITO -> Money.add(saldo, valor)
            MovimentacaoTipos.SAQUE -> Money.subtract(saldo, valor)
            MovimentacaoTipos.TRANSFERENCIA ->
                when (numeroConta) {
                    origemNumero -> Money.subtract(saldo, valor)
                    destinoNumero -> Money.add(saldo, valor)
                    else -> saldo
                }
            else -> saldo
        }
}

object MovimentacaoTipos {
    const val DEPOSITO = "DEPOSITO"
    const val SAQUE = "SAQUE"
    const val TRANSFERENCIA = "TRANSFERENCIA"
}
