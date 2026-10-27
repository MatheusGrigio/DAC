package br.ufpr.dac.bantads.email.mail

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(prefix = "mail", name = ["dev"], havingValue = "false")
@ConditionalOnBean(JavaMailSender::class)
class SmtpMailSender(
    private val javaMailSender: JavaMailSender,
    private val properties: MailProperties,
) : MailSender {
    override fun send(mail: OutgoingMail): String {
        val message = javaMailSender.createMimeMessage()
        val helper = MimeMessageHelper(message, "UTF-8")
        helper.setFrom(properties.from.ifBlank { "bantads@localhost" })
        helper.setTo(mail.to)
        helper.setSubject(mail.subject)
        helper.setText(mail.body, false)
        javaMailSender.send(message)
        return message.messageID?.trim('<', '>') ?: "smtp"
    }
}
