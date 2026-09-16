package br.ufpr.dac.bantads.conta.query.http

import br.ufpr.dac.bantads.shared.money.MoneyJson
import java.math.BigDecimal
import java.time.LocalDate

data class ContaView(
    val numero: String,
    val cpfCliente: String,
    val cpfGerente: String,
    @get:MoneyJson val saldo: BigDecimal,
    val dataCriacao: LocalDate,
)

data class ParteMovimentacaoView(
    val numeroConta: String,
    val cpf: String,
    val nome: String,
)

data class MovimentacaoView(
    val dataHora: String,
    val tipo: String,
    @get:MoneyJson val valor: BigDecimal,
    val origem: ParteMovimentacaoView? = null,
    val destino: ParteMovimentacaoView? = null,
)

data class ExtratoView(
    val numeroConta: String,
    val dataInicio: LocalDate,
    val dataFim: LocalDate,
    @get:MoneyJson val saldoAbertura: BigDecimal,
    val movimentacoes: List<MovimentacaoView>,
)

data class SaldoInternoView(
    @get:MoneyJson val saldo: BigDecimal,
    val numero: String,
    val cpfGerente: String,
)

data class InternalContaView(
    val numero: String,
    val cpfCliente: String,
)
