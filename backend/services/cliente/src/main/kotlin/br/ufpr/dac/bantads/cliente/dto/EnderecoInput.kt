package br.ufpr.dac.bantads.cliente.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

data class EnderecoInput(
    @field:NotBlank val logradouro: String,
    @field:NotBlank val numero: String,
    val complemento: String? = null,
    @field:NotBlank @field:Pattern(regexp = "^\\d{8}$") val cep: String,
    @field:NotBlank val cidade: String,
    @field:NotBlank @field:Pattern(regexp = "^[A-Z]{2}$") val uf: String,
)
