package br.ufpr.dac.bantads.saga.amqp

import br.ufpr.dac.bantads.saga.engine.SagaEngine
import br.ufpr.dac.bantads.shared.amqp.MessageEnvelope
import br.ufpr.dac.bantads.shared.amqp.QueueNames
import br.ufpr.dac.bantads.shared.json.BantadsJson
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component

@Component
class DlqListener(
    private val engine: SagaEngine,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val mapper = BantadsJson.mapper()

    @RabbitListener(
        queues = [
            QueueNames.MS_CLIENTE_CMD_DLQ,
            QueueNames.MS_CONTA_CMD_DLQ,
            QueueNames.MS_GERENTE_CMD_DLQ,
            QueueNames.MS_AUTH_CMD_DLQ,
        ],
    )
    fun onDlq(body: String) {
        try {
            val envelope: MessageEnvelope = mapper.readValue(body)
            log.info("cmd.dlq tipo={} sagaId={}", envelope.tipo, envelope.sagaId)
            engine.onDlq(envelope)
        } catch (ex: Exception) {
            log.warn("cmd.dlq failed: {}", ex.message)
        }
    }
}
