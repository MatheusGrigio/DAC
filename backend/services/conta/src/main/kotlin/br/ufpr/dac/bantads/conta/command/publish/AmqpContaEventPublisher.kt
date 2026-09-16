package br.ufpr.dac.bantads.conta.command.publish

import br.ufpr.dac.bantads.conta.command.event.StoredEvent
import br.ufpr.dac.bantads.shared.amqp.QueueNames
import br.ufpr.dac.bantads.shared.json.BantadsJson
import br.ufpr.dac.bantads.shared.time.DateTimes
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.ObjectProvider

class AmqpContaEventPublisher(
    private val rabbitTemplate: ObjectProvider<RabbitTemplate>,
) : ContaEventPublisher {
    private val log = LoggerFactory.getLogger(javaClass)
    private val mapper = BantadsJson.mapper()

    override fun publish(events: List<StoredEvent>) {
        val template = rabbitTemplate.ifAvailable ?: return
        events.forEach { event ->
            try {
                val body =
                    mapper.writeValueAsString(
                        mapOf(
                            "id" to event.id.toString(),
                            "objetoId" to event.objetoId,
                            "tipo" to event.tipo,
                            "payload" to event.payload,
                            "versao" to event.versao,
                            "timestamp" to DateTimes.format(event.timestamp),
                        ),
                    )
                template.convertAndSend(QueueNames.MS_CONTA_EVENTS, body)
            } catch (ex: Exception) {
                log.warn("failed to publish conta event tipo={} id={}", event.tipo, event.id, ex)
            }
        }
    }
}
