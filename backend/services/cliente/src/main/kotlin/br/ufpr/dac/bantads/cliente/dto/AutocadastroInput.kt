package br.ufpr.dac.bantads.cliente.dto

import br.ufpr.dac.bantads.shared.money.MoneyJson
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import java.math.BigDecimal

data class AutocadastroInput(
    @field:NotBlank @field:Pattern(regexp = "^\\d{11}$") val cpf: String,
    @field:NotBlank val nome: String,
    @field:NotBlank @field:Email val email: String,
    @field:NotBlank val telefone: String,
    @field:NotNull @get:MoneyJson val salario: BigDecimal,
    @field:Valid @field:NotNull val endereco: EnderecoInput,
)
