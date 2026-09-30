package br.ufpr.dac.bantads.email.config

import org.springframework.amqp.core.Queue
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RabbitMQConfig {

    @Bean
    fun emailQueue(): Queue {
        return Queue("ms.email.cmd", true)
    }
}
