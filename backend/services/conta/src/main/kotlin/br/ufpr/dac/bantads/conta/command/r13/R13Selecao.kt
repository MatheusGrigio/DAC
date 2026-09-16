package br.ufpr.dac.bantads.conta.command.r13

import br.ufpr.dac.bantads.conta.command.event.AccountState
import java.math.BigDecimal

data class ContaCandidata(
    val numero: String,
    val cpfCliente: String,
    val cpfGerente: String,
    val saldo: BigDecimal,
)

object R13Selecao {
    fun escolher(
        contas: List<AccountState>,
        gerentesAtivos: Set<String>,
    ): ContaCandidata? {
        val vivas =
            contas
                .filter { it.existe && it.cpfGerente != null && it.cpfCliente != null }
                .filter { it.cpfGerente in gerentesAtivos }
                .map {
                    ContaCandidata(
                        numero = it.numero,
                        cpfCliente = it.cpfCliente!!,
                        cpfGerente = it.cpfGerente!!,
                        saldo = it.saldo,
                    )
                }
        if (vivas.isEmpty()) return null
        val porGerente = vivas.groupBy { it.cpfGerente }
        val maxQtd = porGerente.values.maxOf { it.size }
        if (maxQtd <= 1) return null
        val empatados = porGerente.filterValues { it.size == maxQtd }
        val escolhido =
            empatados.minBy { (_, contasGerente) -> contasGerente.fold(BigDecimal.ZERO) { acc, c -> acc.add(c.saldo) } }
        return escolhido.value.minBy { it.saldo }
    }
}
