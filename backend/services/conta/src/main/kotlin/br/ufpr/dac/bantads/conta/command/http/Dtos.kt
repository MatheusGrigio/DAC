package br.ufpr.dac.bantads.conta.command.http

import br.ufpr.dac.bantads.shared.money.MoneyJson
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import java.math.BigDecimal

data class OperacaoInput(
    @field:NotNull @get:MoneyJson val valor: BigDecimal,
)

data class ParteTransferenciaInput(
    @field:NotBlank @field:Pattern(regexp = "^\\d{4}$") val numeroConta: String,
    @field:NotBlank @field:Pattern(regexp = "^\\d{11}$") val cpf: String,
    @field:NotBlank val nome: String,
)

data class TransferenciaCommandInput(
    @field:NotNull @get:MoneyJson val valor: BigDecimal,
    @field:Valid @field:NotNull val origem: ParteTransferenciaInput,
    @field:Valid @field:NotNull val destino: ParteTransferenciaInput,
)

data class ParteTransferenciaView(
    val numeroConta: String,
    val cpf: String,
    val nome: String,
)

data class OperacaoRealizadaView(
    val numeroConta: String,
    val tipo: String,
    val dataHora: String,
    @get:MoneyJson val valor: BigDecimal,
    val destino: ParteTransferenciaView? = null,
)
