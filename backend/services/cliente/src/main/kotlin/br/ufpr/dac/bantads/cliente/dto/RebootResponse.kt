package br.ufpr.dac.bantads.cliente.dto

data class RebootResponse(
    val status: String = "ok",
    val clientes: Int,
)
