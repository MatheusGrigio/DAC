package br.ufpr.dac.bantads.conta.command.store

import br.ufpr.dac.bantads.conta.command.event.AccountState
import br.ufpr.dac.bantads.conta.command.event.EventReplay
import br.ufpr.dac.bantads.conta.command.event.StoredEvent
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.UUID

@Component
class EventStore(
    private val eventos: EventoRepository,
) {
    fun load(numero: String): List<StoredEvent> = eventos.findByObjetoIdOrderByVersaoAsc(numero).map { it.toStored() }

    fun state(numero: String): AccountState = EventReplay.apply(numero, load(numero))

    fun exists(numero: String): Boolean = eventos.existsByObjetoId(numero)

    fun allStates(): List<AccountState> =
        eventos.findAll().groupBy { it.objetoId }.map { (numero, lista) ->
            EventReplay.apply(numero, lista.map { it.toStored() }.sortedBy { it.versao })
        }

    fun append(event: StoredEvent): EventoEntity =
        eventos.saveAndFlush(
            EventoEntity(
                id = event.id,
                objetoId = event.objetoId,
                tipo = event.tipo,
                payload = event.payload,
                versao = event.versao,
                timestamp = event.timestamp,
            ),
        )

    fun deleteStream(numero: String) {
        eventos.deleteByObjetoId(numero)
    }

    fun newEvent(
        numero: String,
        tipo: String,
        payload: Map<String, Any?>,
        versao: Int,
        timestamp: LocalDateTime,
    ) = StoredEvent(
        id = UUID.randomUUID(),
        objetoId = numero,
        tipo = tipo,
        payload = payload,
        versao = versao,
        timestamp = timestamp,
    )
}

fun EventoEntity.toStored() =
    StoredEvent(
        id = id,
        objetoId = objetoId,
        tipo = tipo,
        payload = payload,
        versao = versao,
        timestamp = timestamp,
    )
