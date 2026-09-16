package br.ufpr.dac.bantads.conta.query.store

import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime
import java.util.UUID

interface MovimentacaoRepository : JpaRepository<MovimentacaoEntity, UUID> {
    fun findByNumeroContaAndDataHoraGreaterThanEqualAndDataHoraLessThanOrderByDataHoraAsc(
        numeroConta: String,
        inicio: LocalDateTime,
        fimExclusivo: LocalDateTime,
    ): List<MovimentacaoEntity>

    fun findByNumeroContaAndDataHoraLessThanOrderByDataHoraAsc(
        numeroConta: String,
        limite: LocalDateTime,
    ): List<MovimentacaoEntity>
}
