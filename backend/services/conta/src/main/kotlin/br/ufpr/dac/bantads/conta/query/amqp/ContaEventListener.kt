package br.ufpr.dac.bantads.conta.query.amqp

import br.ufpr.dac.bantads.conta.command.event.StoredEvent
import br.ufpr.dac.bantads.conta.query.project.EventProjector
import br.ufpr.dac.bantads.shared.amqp.QueueNames
import br.ufpr.dac.bantads.shared.json.BantadsJson
import br.ufpr.dac.bantads.shared.time.DateTimes
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ContaEventListener(
    private val projector: EventProjector,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val mapper = BantadsJson.mapper()

    @RabbitListener(queues = [QueueNames.MS_CONTA_EVENTS])
    fun onEvent(body: String) {
        val message = mapper.readValue<ContaEventMessage>(body)
        log.info("conta event tipo={} id={} conta={}", message.tipo, message.id, message.objetoId)
        projector.apply(
            StoredEvent(
                id = message.id,
                objetoId = message.objetoId,
                tipo = message.tipo,
                payload = message.payload,
                versao = message.versao,
                timestamp = DateTimes.parse(message.timestamp),
            ),
        )
    }
}

data class ContaEventMessage(
    val id: UUID,
    val objetoId: String,
    val tipo: String,
    val payload: Map<String, Any?>,
    val versao: Int,
    val timestamp: String,
)
