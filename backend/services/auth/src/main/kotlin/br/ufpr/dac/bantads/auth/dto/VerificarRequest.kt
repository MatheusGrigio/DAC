package br.ufpr.dac.bantads.auth.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class VerificarRequest(
    @field:NotBlank @field:Email val email: String,
    @field:NotBlank val senha: String,
)
