package br.ufpr.dac.bantads.gerente.dto

import jakarta.validation.constraints.NotBlank

data class GerenteUpdate(
    @field:NotBlank val nome: String,
    @field:NotBlank val telefone: String,
    val cpf: String? = null,
    val email: String? = null,
)

data class GerenteView(
    val cpf: String,
    val nome: String,
    val email: String,
    val telefone: String,
    val ativo: Boolean,
    val quantidadeClientes: Int? = null,
)

data class RebootResponse(
    val status: String = "ok",
    val gerentes: Int,
)
