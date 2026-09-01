package br.ufpr.dac.bantads.cliente.saga

import br.ufpr.dac.bantads.shared.amqp.MessageEnvelope
import br.ufpr.dac.bantads.shared.amqp.QueueNames
import br.ufpr.dac.bantads.shared.json.BantadsJson
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Component

@Component
class ClienteCommandListener(
    private val handler: ClienteCommandHandler,
    private val rabbitTemplate: ObjectProvider<RabbitTemplate>,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val mapper = BantadsJson.mapper()

    @RabbitListener(queues = [QueueNames.MS_CLIENTE_CMD])
    fun onCommand(body: String) {
        val envelope: MessageEnvelope = mapper.readValue(body)
        log.info("cliente cmd tipo={} sagaId={}", envelope.tipo, envelope.sagaId)
        val reply = handler.handle(envelope)
        if (envelope.sagaId != null) {
            rabbitTemplate.ifAvailable?.convertAndSend(
                QueueNames.ORQUESTRADOR_REPLY,
                mapper.writeValueAsString(reply),
            )
        }
    }
}
