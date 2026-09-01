package br.ufpr.dac.bantads.cliente.dto

import br.ufpr.dac.bantads.shared.money.MoneyJson
import java.math.BigDecimal
import java.time.LocalDateTime

data class EnderecoView(
    val logradouro: String,
    val numero: String,
    val complemento: String?,
    val cep: String,
    val cidade: String,
    val uf: String,
)

data class SolicitacaoView(
    val cpf: String,
    val nome: String,
    val email: String,
    val telefone: String,
    @get:MoneyJson val salario: BigDecimal,
    val endereco: EnderecoView,
    val status: String,
    val motivo: String?,
    val dataHoraProcessamento: LocalDateTime?,
)

data class ClienteView(
    val cpf: String,
    val nome: String,
    val email: String,
    val telefone: String,
    @get:MoneyJson val salario: BigDecimal,
    val endereco: EnderecoView,
)

data class ClienteNomeView(
    val cpf: String,
    val nome: String,
    val email: String,
)
