package br.ufpr.dac.bantads.email.mail

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "mail")
data class MailProperties(
    val dev: Boolean = true,
    val outboxDir: String = "/tmp/outbox",
    val from: String = "bantads@localhost",
)
