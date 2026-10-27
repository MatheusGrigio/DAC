package br.ufpr.dac.bantads.email.send

import br.ufpr.dac.bantads.email.compose.Destinatarios
import br.ufpr.dac.bantads.email.compose.EmailComposer
import br.ufpr.dac.bantads.email.mail.MailSender
import br.ufpr.dac.bantads.shared.amqp.CommandTypes
import br.ufpr.dac.bantads.shared.amqp.MessageEnvelope
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class EmailCommandService(
    private val sender: MailSender,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val conhecidos =
        setOf(
            CommandTypes.EMAIL_SENHA_CLIENTE,
            CommandTypes.EMAIL_FALHA_APROVACAO,
            CommandTypes.EMAIL_REJEICAO,
            CommandTypes.EMAIL_TROCA_GERENTE,
        )

    fun handle(envelope: MessageEnvelope) {
        if (envelope.tipo !in conhecidos) {
            log.warn("email cmd tipo desconhecido={}", envelope.tipo)
            return
        }
        val destinos = Destinatarios.from(envelope.payload)
        if (destinos.isEmpty()) {
            log.warn("email cmd tipo={} sem destinatario", envelope.tipo)
            return
        }
        destinos.forEach { dest ->
            val mail = EmailComposer.compose(envelope.tipo, envelope.payload, dest)
            val messageId = sender.send(mail)
            log.info("email sent tipo={} to={} messageId={}", envelope.tipo, dest.email, messageId)
        }
    }
}
