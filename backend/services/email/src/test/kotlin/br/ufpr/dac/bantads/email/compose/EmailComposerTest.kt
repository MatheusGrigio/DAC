package br.ufpr.dac.bantads.email.compose

import br.ufpr.dac.bantads.email.mail.Destinatario
import br.ufpr.dac.bantads.shared.amqp.CommandTypes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EmailComposerTest {
    @Test
    fun `senha-cliente body contains password and not extra payload dump`() {
        val dest =
            Destinatarios
                .from(mapOf("email" to "cli@bantads.com.br", "nome" to "Ana", "senha" to "s3nh4"))
                .single()
        val mail =
            EmailComposer.compose(
                CommandTypes.EMAIL_SENHA_CLIENTE,
                mapOf("senha" to "s3nh4", "nome" to "Ana"),
                dest,
            )
        assertEquals("cli@bantads.com.br", mail.to)
        assertTrue(mail.subject.contains("senha", ignoreCase = true))
        assertTrue(mail.body.contains("s3nh4"))
        assertEquals("s3nh4", mail.senha)
    }

    @Test
    fun `rejeicao includes motivo`() {
        val dest = Destinatario("a@b.com", "Ana")
        val mail =
            EmailComposer.compose(
                CommandTypes.EMAIL_REJEICAO,
                mapOf("motivo" to "renda insuficiente"),
                dest,
            )
        assertTrue(mail.body.contains("renda insuficiente"))
    }

    @Test
    fun `troca-gerente accepts many recipients`() {
        val dests =
            Destinatarios.from(
                mapOf(
                    "destinatarios" to
                        listOf(
                            mapOf("email" to "a@b.com", "nome" to "A"),
                            mapOf("email" to "c@d.com", "nome" to "C"),
                        ),
                    "nomeGerente" to "Geniéve",
                ),
            )
        assertEquals(2, dests.size)
        val mail =
            EmailComposer.compose(
                CommandTypes.EMAIL_TROCA_GERENTE,
                mapOf("nomeGerente" to "Geniéve"),
                dests.first(),
            )
        assertTrue(mail.body.contains("Geniéve"))
    }
}
