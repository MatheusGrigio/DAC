package br.ufpr.dac.bantads.saga.amqp

import br.ufpr.dac.bantads.saga.engine.SagaEngine
import br.ufpr.dac.bantads.shared.amqp.QueueNames
import br.ufpr.dac.bantads.shared.amqp.ReplyEnvelope
import br.ufpr.dac.bantads.shared.json.BantadsJson
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component

@Component
class ReplyListener(
    private val engine: SagaEngine,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val mapper = BantadsJson.mapper()

    @RabbitListener(queues = [QueueNames.ORQUESTRADOR_REPLY])
    fun onReply(body: String) {
        try {
            val reply: ReplyEnvelope = mapper.readValue(body)
            log.info("orquestrador.reply tipo={} sagaId={} status={}", reply.tipo, reply.sagaId, reply.status)
            engine.onReply(reply)
        } catch (ex: Exception) {
            log.warn("orquestrador.reply failed: {}", ex.message)
        }
    }
}
