package br.ufpr.dac.bantads.cliente.dto

import jakarta.validation.constraints.NotBlank

data class RejeicaoInput(
    @field:NotBlank val motivo: String,
)
