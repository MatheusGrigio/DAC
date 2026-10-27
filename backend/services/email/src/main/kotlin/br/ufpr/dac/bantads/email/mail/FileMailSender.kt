package br.ufpr.dac.bantads.email.mail

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

@Component
@ConditionalOnProperty(prefix = "mail", name = ["dev"], havingValue = "true", matchIfMissing = true)
class FileMailSender(
    private val properties: MailProperties,
) : MailSender {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun send(mail: OutgoingMail): String {
        val dir = Path.of(properties.outboxDir)
        Files.createDirectories(dir)
        val file = dir.resolve(safeFileName(mail.to))
        val messageId = UUID.randomUUID().toString()
        val senhaLine = mail.senha?.let { "senha: $it\n" }.orEmpty()
        val content =
            "message-id: $messageId\n" +
                "to: ${mail.to}\n" +
                "from: ${properties.from}\n" +
                "subject: ${mail.subject}\n" +
                senhaLine +
                "\n${mail.body}\n"
        Files.writeString(file, content)
        log.info("MAIL_DEV outbox file={}", file.toAbsolutePath())
        return messageId
    }

    private fun safeFileName(to: String): String {
        val safe = to.replace(Regex("""[<>:"/\\|?*]"""), "_")
        return "$safe.txt"
    }
}
