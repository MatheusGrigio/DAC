package br.ufpr.dac.bantads.cliente.email

import br.ufpr.dac.bantads.shared.amqp.CommandTypes
import br.ufpr.dac.bantads.shared.amqp.MessageEnvelope
import br.ufpr.dac.bantads.shared.amqp.QueueNames
import br.ufpr.dac.bantads.shared.json.BantadsJson
import br.ufpr.dac.bantads.shared.time.DateTimes
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.ObjectProvider

class AmqpEmailCommandPublisher(
    private val rabbitTemplate: ObjectProvider<RabbitTemplate>,
) : EmailCommandPublisher {
    private val log = LoggerFactory.getLogger(javaClass)
    private val mapper = BantadsJson.mapper()

    override fun publishRejeicao(
        email: String,
        nome: String,
        motivo: String,
    ) {
        val envelope =
            MessageEnvelope(
                tipo = CommandTypes.EMAIL_REJEICAO,
                timestamp = DateTimes.now(),
                payload = mapOf("email" to email, "nome" to nome, "motivo" to motivo),
            )
        val template = rabbitTemplate.ifAvailable ?: return
        try {
            template.convertAndSend(QueueNames.MS_EMAIL_CMD, mapper.writeValueAsString(envelope))
        } catch (ex: Exception) {
            log.warn("failed to publish email.rejeicao: {}", ex.message)
        }
    }
}
