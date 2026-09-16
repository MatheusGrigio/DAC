package br.ufpr.dac.bantads.conta.command.publish

import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.ObjectProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ContaEventPublisherConfig {
    @Bean
    fun contaEventPublisher(rabbitTemplate: ObjectProvider<RabbitTemplate>): ContaEventPublisher = AmqpContaEventPublisher(rabbitTemplate)
}
