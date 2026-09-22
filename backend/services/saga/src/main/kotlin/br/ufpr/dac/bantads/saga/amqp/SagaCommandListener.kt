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
class SagaCommandListener(
    private val engine: SagaEngine,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val mapper = BantadsJson.mapper()

    @RabbitListener(queues = [QueueNames.SAGA_CMD])
    fun onCommand(body: String) {
        try {
            val envelope: MessageEnvelope = mapper.readValue(body)
            log.info("saga.cmd tipo={} sagaId={}", envelope.tipo, envelope.sagaId)
            engine.start(envelope)
        } catch (ex: Exception) {
            log.warn("saga.cmd failed: {}", ex.message)
        }
    }
}
