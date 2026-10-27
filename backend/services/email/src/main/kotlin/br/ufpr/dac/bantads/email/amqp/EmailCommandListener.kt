package br.ufpr.dac.bantads.email.amqp

import br.ufpr.dac.bantads.email.send.EmailCommandService
import br.ufpr.dac.bantads.shared.amqp.MessageEnvelope
import br.ufpr.dac.bantads.shared.amqp.QueueNames
import br.ufpr.dac.bantads.shared.json.BantadsJson
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component

@Component
class EmailCommandListener(
    private val commands: EmailCommandService,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val mapper = BantadsJson.mapper()

    @RabbitListener(queues = [QueueNames.MS_EMAIL_CMD])
    fun onCommand(body: String) {
        try {
            val envelope: MessageEnvelope = mapper.readValue(body)
            log.info("email cmd tipo={} sagaId={}", envelope.tipo, envelope.sagaId)
            commands.handle(envelope)
        } catch (ex: Exception) {
            log.warn("email cmd failed: {}", ex.message)
        }
    }
}
