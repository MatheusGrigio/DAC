package br.ufpr.dac.bantads.conta.command.publish

import br.ufpr.dac.bantads.conta.command.event.StoredEvent

interface ContaEventPublisher {
    fun publish(events: List<StoredEvent>)
}
