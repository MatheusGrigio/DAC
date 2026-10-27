package br.ufpr.dac.bantads.shared.amqp

import org.springframework.amqp.AmqpRejectAndDontRequeueException
import org.springframework.amqp.ImmediateAcknowledgeAmqpException
import org.springframework.amqp.core.Message
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.util.ErrorHandler

@Configuration
@ConditionalOnProperty(prefix = "bantads.rabbit", name = ["retry-dlq"], havingValue = "true")
class BantadsRabbitRetryConfiguration {
    @Bean
    fun rabbitListenerContainerFactory(
        connectionFactory: ConnectionFactory,
        rabbitTemplate: RabbitTemplate,
    ): SimpleRabbitListenerContainerFactory {
        val factory = SimpleRabbitListenerContainerFactory()
        factory.setConnectionFactory(connectionFactory)
        factory.setDefaultRequeueRejected(false)
        factory.setErrorHandler(DeadLetterAfterRetries(rabbitTemplate))
        return factory
    }
}

class DeadLetterAfterRetries(
    private val rabbit: RabbitTemplate,
) : ErrorHandler {
    override fun handleError(throwable: Throwable) {
        val failed = failedMessage(throwable)
        val queue = failed?.messageProperties?.consumerQueue
        if (failed == null || queue.isNullOrBlank()) {
            throw AmqpRejectAndDontRequeueException("falha tecnica sem fila", throwable)
        }
        val deaths = failed.messageProperties.headers["x-death"] as? List<*>
        if (XDeathCounts.count(deaths, queue) >= XDeathCounts.DLQ_AT) {
            rabbit.send("$queue.dlq", failed)
            throw ImmediateAcknowledgeAmqpException("encaminhado para $queue.dlq")
        }
        throw AmqpRejectAndDontRequeueException("retentativa", throwable)
    }

    private fun failedMessage(throwable: Throwable): Message? {
        var current: Throwable? = throwable
        while (current != null) {
            if (current is ListenerExecutionFailedException) return current.failedMessage
            current = current.cause
        }
        return null
    }
}
