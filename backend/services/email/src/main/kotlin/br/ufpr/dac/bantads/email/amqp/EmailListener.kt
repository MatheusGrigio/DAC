package br.ufpr.dac.bantads.email.amqp

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Component

@Component
class EmailListener(
    private val mailSender: JavaMailSender,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(EmailListener::class.java)

    @RabbitListener(queues = ["ms.email.cmd"])
    fun receiveEmailCommand(message: String) {
        try {
            val jsonNode = objectMapper.readTree(message)
            val payload = jsonNode.get("payload")
            
            // The pdf does not strictly define the payload for ms.email.cmd
            // Assuming it has an email, assunto and mensagem or similar fields
            val to = payload.get("email")?.asText()
            val text = payload.get("mensagem")?.asText() ?: payload.toString()
            val subject = payload.get("assunto")?.asText() ?: "BANTADS Notificação"

            if (to != null) {
                val mailMessage = SimpleMailMessage()
                mailMessage.setTo(to)
                mailMessage.subject = subject
                mailMessage.text = text
                
                mailSender.send(mailMessage)
                logger.info("E-mail successfully sent to \$to")
            } else {
                logger.warn("Payload did not contain 'email' field: \$payload")
            }
        } catch (e: Exception) {
            logger.error("Error processing email message: \$message", e)
        }
    }
}
