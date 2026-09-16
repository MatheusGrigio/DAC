package br.ufpr.dac.bantads.auth

import br.ufpr.dac.bantads.auth.saga.AuthCommandHandler
import br.ufpr.dac.bantads.auth.seed.SeedUsers
import br.ufpr.dac.bantads.auth.user.UsuarioRepository
import br.ufpr.dac.bantads.shared.amqp.CommandTypes
import br.ufpr.dac.bantads.shared.amqp.MessageEnvelope
import br.ufpr.dac.bantads.shared.amqp.ReplyStatus
import br.ufpr.dac.bantads.shared.domain.Perfil
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class AuthIT {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var usuarios: UsuarioRepository

    @Autowired
    lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    lateinit var commands: AuthCommandHandler

    @BeforeEach
    fun seed() {
        mockMvc.post("/internal/reboot").andExpect { status { isOk() } }
    }

    @Test
    fun `health is UP without links`() {
        mockMvc.get("/health").andExpect {
            status { isOk() }
            jsonPath("$.status") { value("UP") }
            jsonPath("\$._links") { doesNotExist() }
        }
    }

    @Test
    fun `verificar seed client returns cpf and tipo without hash`() {
        val body =
            mockMvc
                .post("/auth/verificar") {
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"email":"cli1@bantads.com.br","senha":"tads"}"""
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.cpf") { value("12912861012") }
                    jsonPath("$.tipo") { value(Perfil.CLIENTE.wire) }
                    jsonPath("$.senhaHash") { doesNotExist() }
                    jsonPath("$.senha") { doesNotExist() }
                }.andReturn()
                .response.contentAsString
        assertTrue(!body.contains("argon", ignoreCase = true))
    }

    @Test
    fun `wrong password or unknown email is 401`() {
        mockMvc
            .post("/auth/verificar") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"email":"cli1@bantads.com.br","senha":"errada"}"""
            }.andExpect { status { isUnauthorized() } }
        mockMvc
            .post("/auth/verificar") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"email":"nobody@bantads.com.br","senha":"tads"}"""
            }.andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `inactive user cannot authenticate`() {
        val reply =
            commands.handle(
                MessageEnvelope(
                    sagaId = "saga-desativar",
                    tipo = CommandTypes.AUTH_DESATIVAR,
                    timestamp = "2026-04-30T10:00:00",
                    payload = mapOf("cpf" to "12912861012"),
                ),
            )
        assertEquals(ReplyStatus.SUCESSO, reply.status)
        mockMvc
            .post("/auth/verificar") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"email":"cli1@bantads.com.br","senha":"tads"}"""
            }.andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `reboot twice restores the same nine seed users`() {
        commands.handle(
            MessageEnvelope(
                sagaId = "saga-desativar-reboot",
                tipo = CommandTypes.AUTH_DESATIVAR,
                timestamp = "2026-04-30T10:00:00",
                payload = mapOf("cpf" to "12912861012"),
            ),
        )
        mockMvc.post("/internal/reboot").andExpect {
            status { isOk() }
            jsonPath("$.status") { value("ok") }
            jsonPath("$.usuarios") { value(SeedUsers.ALL.size) }
            jsonPath("$._links") { doesNotExist() }
        }
        mockMvc.post("/internal/reboot").andExpect {
            status { isOk() }
            jsonPath("$.usuarios") { value(SeedUsers.ALL.size) }
        }
        assertEquals(SeedUsers.ALL.map { it.cpf }.toSet(), usuarios.findAll().map { it.cpf }.toSet())
        assertTrue(usuarios.findAll().all { it.ativo })
        mockMvc
            .post("/auth/verificar") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"email":"cli1@bantads.com.br","senha":"tads"}"""
            }.andExpect {
                status { isOk() }
                jsonPath("$.cpf") { value("12912861012") }
            }
    }

    @Test
    fun `stored hash is not the plaintext password`() {
        val user = checkNotNull(usuarios.findByCpf("12912861012"))
        assertNotEquals(SeedUsers.SENHA, user.senhaHash)
        assertTrue(passwordEncoder.matches(SeedUsers.SENHA, user.senhaHash))
        assertTrue(user.senhaHash.startsWith("\$argon2id\$"))
    }

    @Test
    fun `duplicate login fails with exact error`() {
        val first =
            commands.handle(
                MessageEnvelope(
                    sagaId = "saga-criar-1",
                    tipo = CommandTypes.AUTH_CRIAR_CLIENTE,
                    timestamp = "2026-04-30T10:00:00",
                    payload = mapOf("cpf" to "11111111111", "email" to "novo@bantads.com.br"),
                ),
            )
        assertEquals(ReplyStatus.SUCESSO, first.status)
        assertTrue(first.payload["senha"] is String)
        val second =
            commands.handle(
                MessageEnvelope(
                    sagaId = "saga-criar-2",
                    tipo = CommandTypes.AUTH_CRIAR_CLIENTE,
                    timestamp = "2026-04-30T10:00:00",
                    payload = mapOf("cpf" to "22222222222", "email" to "novo@bantads.com.br"),
                ),
            )
        assertEquals(ReplyStatus.FALHA, second.status)
        assertEquals("E-mail já cadastrado", second.erro)
    }

    @Test
    fun `criar cliente is idempotent on sagaId and tipo`() {
        val envelope =
            MessageEnvelope(
                sagaId = "saga-idemp",
                tipo = CommandTypes.AUTH_CRIAR_CLIENTE,
                timestamp = "2026-04-30T10:00:00",
                payload = mapOf("cpf" to "33333333333", "email" to "idemp@bantads.com.br"),
            )
        val first = commands.handle(envelope)
        val second = commands.handle(envelope)
        assertEquals(ReplyStatus.SUCESSO, first.status)
        assertEquals(first.payload["senha"], second.payload["senha"])
        assertEquals(1, usuarios.findAll().count { it.login == "idemp@bantads.com.br" })
    }

    @Test
    fun `malformed verificar is 400`() {
        mockMvc
            .post("/auth/verificar") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"email":"cli1@bantads.com.br"}"""
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.status") { value(400) }
                jsonPath("$.erro") { value("Bad Request") }
            }
    }

    companion object {
        @Container
        @JvmField
        val mongo = MongoDBContainer(DockerImageName.parse("mongo:7"))

        @JvmStatic
        @DynamicPropertySource
        fun mongoProps(registry: DynamicPropertyRegistry) {
            registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl)
            registry.add("spring.data.mongodb.host") { "" }
            registry.add("spring.data.mongodb.username") { "" }
            registry.add("spring.data.mongodb.password") { "" }
        }
    }
}
