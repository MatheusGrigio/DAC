package br.ufpr.dac.bantads.conta.command.publish

import br.ufpr.dac.bantads.shared.amqp.QueueNames
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.ObjectProvider
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class OutboxRelay(
    private val rabbit: ObjectProvider<RabbitTemplate>,
    private val outbox: EventOutboxRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelay = 500)
    fun publishPending() {
        val template = rabbit.ifAvailable ?: return
        outbox.findTop50ByPublishedFalseOrderByCreatedAtAsc().forEach { row ->
            try {
                template.convertAndSend(QueueNames.MS_CONTA_EVENTS, row.body)
                row.published = true
                outbox.save(row)
            } catch (ex: Exception) {
                log.warn("outbox publish failed id={}", row.id, ex)
            }
        }
    }
}
