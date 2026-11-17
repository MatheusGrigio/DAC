package br.ufpr.dac.bantads.saga

import br.ufpr.dac.bantads.shared.amqp.QueueNames
import org.springframework.amqp.core.Queue
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@TestConfiguration
class TestAmqpQueues {
    @Bean
    fun sagaCmd(): Queue = Queue(QueueNames.SAGA_CMD, true)

    @Bean
    fun reply(): Queue = Queue(QueueNames.ORQUESTRADOR_REPLY, true)

    @Bean
    fun authCmd(): Queue = Queue(QueueNames.MS_AUTH_CMD, true)

    @Bean
    fun authDlq(): Queue = Queue(QueueNames.MS_AUTH_CMD_DLQ, true)

    @Bean
    fun clienteDlq(): Queue = Queue(QueueNames.MS_CLIENTE_CMD_DLQ, true)

    @Bean
    fun gerenteDlq(): Queue = Queue(QueueNames.MS_GERENTE_CMD_DLQ, true)

    @Bean
    fun contaDlq(): Queue = Queue(QueueNames.MS_CONTA_CMD_DLQ, true)
}
