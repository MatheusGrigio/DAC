package br.ufpr.dac.bantads.saga.amqp

import br.ufpr.dac.bantads.saga.engine.CommandBus
import br.ufpr.dac.bantads.shared.amqp.MessageEnvelope
import br.ufpr.dac.bantads.shared.json.BantadsJson
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Component

@Component
class RabbitCommandBus(
    private val rabbitTemplate: RabbitTemplate,
) : CommandBus {
    private val mapper = BantadsJson.mapper()

    override fun publish(
        queue: String,
        envelope: MessageEnvelope,
    ) {
        rabbitTemplate.convertAndSend(queue, mapper.writeValueAsString(envelope))
    }
}
