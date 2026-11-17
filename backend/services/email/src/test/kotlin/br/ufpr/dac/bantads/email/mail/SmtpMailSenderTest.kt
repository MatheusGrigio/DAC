package br.ufpr.dac.bantads.email.mail

import com.icegreen.greenmail.junit5.GreenMailExtension
import com.icegreen.greenmail.util.GreenMailUtil
import com.icegreen.greenmail.util.ServerSetupTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.mail.javamail.JavaMailSenderImpl

class SmtpMailSenderTest {
    @Test
    fun `smtp send delivers body with password without putting it in subject`() {
        greenMail.setUser("bantads@localhost", "secret")
        val javaMail =
            JavaMailSenderImpl().apply {
                host = "127.0.0.1"
                port = greenMail.smtp.port
                username = "bantads@localhost"
                password = "secret"
            }
        val sender = SmtpMailSender(javaMail, MailProperties(dev = false, from = "bantads@localhost"))
        val id =
            sender.send(
                OutgoingMail(
                    to = "cli@bantads.com.br",
                    subject = "BANTADS — sua senha de acesso",
                    body = "Senha de acesso: s3nh4\n",
                    senha = "s3nh4",
                ),
            )
        assertTrue(id.isNotBlank())
        val received = greenMail.receivedMessages
        assertEquals(1, received.size)
        assertEquals("BANTADS — sua senha de acesso", received[0].subject)
        assertTrue(GreenMailUtil.getBody(received[0]).contains("s3nh4"))
    }

    companion object {
        @RegisterExtension
        @JvmField
        val greenMail = GreenMailExtension(ServerSetupTest.SMTP)
    }
}
