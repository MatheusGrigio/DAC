package br.ufpr.dac.bantads.saga.engine

import br.ufpr.dac.bantads.shared.amqp.MessageEnvelope

fun interface CommandBus {
    fun publish(
        queue: String,
        envelope: MessageEnvelope,
    )
}
