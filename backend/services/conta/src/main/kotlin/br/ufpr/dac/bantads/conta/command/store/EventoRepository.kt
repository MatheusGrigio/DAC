package br.ufpr.dac.bantads.conta.command.store

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface EventoRepository : JpaRepository<EventoEntity, UUID> {
    fun findByObjetoIdOrderByVersaoAsc(objetoId: String): List<EventoEntity>

    fun existsByObjetoId(objetoId: String): Boolean

    fun deleteByObjetoId(objetoId: String)
}
