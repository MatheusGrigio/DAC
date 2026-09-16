package br.ufpr.dac.bantads.auth.dto

data class RebootResponse(
    val status: String = "ok",
    val usuarios: Int,
)
