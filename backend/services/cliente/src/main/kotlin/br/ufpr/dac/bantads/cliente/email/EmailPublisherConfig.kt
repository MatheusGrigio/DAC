package br.ufpr.dac.bantads.cliente.email

import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.ObjectProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class EmailPublisherConfig {
    @Bean
    fun emailCommandPublisher(rabbitTemplate: ObjectProvider<RabbitTemplate>): EmailCommandPublisher =
        AmqpEmailCommandPublisher(rabbitTemplate)
}
