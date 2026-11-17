package br.ufpr.dac.bantads.email

import br.ufpr.dac.bantads.email.send.EmailCommandService
import br.ufpr.dac.bantads.shared.amqp.CommandTypes
import br.ufpr.dac.bantads.shared.amqp.MessageEnvelope
import br.ufpr.dac.bantads.shared.time.DateTimes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.nio.file.Files
import java.nio.file.Path

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmailIT {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var commands: EmailCommandService

    @BeforeEach
    fun cleanOutbox() {
        Files.createDirectories(outbox)
        Files.list(outbox).use { stream -> stream.forEach { Files.deleteIfExists(it) } }
    }

    @Test
    fun `health is UP without links`() {
        mockMvc.get("/health").andExpect {
            status { isOk() }
            jsonPath("$.status") { value("UP") }
            jsonPath("$._links") { doesNotExist() }
        }
    }

    @Test
    fun `MAIL_DEV outbox exposes R9 password`() {
        commands.handle(
            MessageEnvelope(
                sagaId = "saga-r9",
                tipo = CommandTypes.EMAIL_SENHA_CLIENTE,
                timestamp = DateTimes.now(),
                payload = mapOf("email" to "cli9@bantads.com.br", "nome" to "Nova", "senha" to "AbCd12"),
            ),
        )
        val file = outbox.resolve("cli9@bantads.com.br.txt")
        assertTrue(Files.exists(file))
        val text = Files.readString(file)
        assertTrue(text.contains("senha: AbCd12"))
        assertTrue(text.contains("AbCd12"))
        assertTrue(text.contains("message-id:"))
    }

    @Test
    fun `rejeicao and troca-gerente write outbox files`() {
        commands.handle(
            MessageEnvelope(
                tipo = CommandTypes.EMAIL_REJEICAO,
                timestamp = DateTimes.now(),
                payload = mapOf("email" to "rej@bantads.com.br", "nome" to "Rej", "motivo" to "docs"),
            ),
        )
        commands.handle(
            MessageEnvelope(
                tipo = CommandTypes.EMAIL_TROCA_GERENTE,
                timestamp = DateTimes.now(),
                payload =
                    mapOf(
                        "destinatarios" to
                            listOf(
                                mapOf("email" to "a@bantads.com.br", "nome" to "A"),
                                mapOf("email" to "b@bantads.com.br", "nome" to "B"),
                            ),
                        "nomeGerente" to "Geniéve",
                    ),
            ),
        )
        assertTrue(Files.readString(outbox.resolve("rej@bantads.com.br.txt")).contains("docs"))
        assertEquals(true, Files.exists(outbox.resolve("a@bantads.com.br.txt")))
        assertEquals(true, Files.exists(outbox.resolve("b@bantads.com.br.txt")))
    }

    companion object {
        private val outbox: Path = Files.createTempDirectory("bantads-mail-outbox")

        @JvmStatic
        @DynamicPropertySource
        fun outboxDir(registry: DynamicPropertyRegistry) {
            registry.add("mail.outbox-dir") { outbox.toAbsolutePath().toString() }
            registry.add("mail.dev") { "true" }
        }
    }
}
