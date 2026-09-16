package br.ufpr.dac.bantads.conta.command.event

import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

data class StoredEvent(
    val id: UUID,
    val objetoId: String,
    val tipo: String,
    val payload: Map<String, Any?>,
    val versao: Int,
    val timestamp: LocalDateTime,
)

data class AccountState(
    val numero: String,
    val existe: Boolean = false,
    val cpfCliente: String? = null,
    val cpfGerente: String? = null,
    val saldo: BigDecimal = BigDecimal.ZERO.setScale(2),
    val dataCriacao: String? = null,
    val versao: Int = 0,
)
