package br.ufpr.dac.bantads.gerente

import br.ufpr.dac.bantads.gerente.cadastro.GerenteRepository
import br.ufpr.dac.bantads.gerente.cadastro.GerenteRules
import br.ufpr.dac.bantads.gerente.saga.GerenteCommandHandler
import br.ufpr.dac.bantads.gerente.seed.SeedGerentes
import br.ufpr.dac.bantads.shared.amqp.CommandTypes
import br.ufpr.dac.bantads.shared.amqp.MessageEnvelope
import br.ufpr.dac.bantads.shared.amqp.ReplyStatus
import br.ufpr.dac.bantads.shared.domain.Perfil
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockHttpServletRequestDsl
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class GerenteIT {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var commands: GerenteCommandHandler

    @Autowired
    lateinit var gerentes: GerenteRepository

    @BeforeEach
    fun seed() {
        mockMvc.post("/internal/reboot").andExpect { status { isOk() } }
    }

    @Test
    fun `reboot twice restores the four seed gerentes`() {
        mockMvc
            .put("/gerentes/98574307084") {
                gerente("64065268052")
                contentType = MediaType.APPLICATION_JSON
                content = """{"nome":"Geniéve Silva","telefone":"41988889999"}"""
            }.andExpect { status { isOk() } }
        mockMvc.post("/internal/reboot").andExpect {
            status { isOk() }
            jsonPath("$.status") { value("ok") }
            jsonPath("$.gerentes") { value(4) }
            jsonPath("$._links") { doesNotExist() }
        }
        mockMvc.post("/internal/reboot").andExpect {
            status { isOk() }
            jsonPath("$.gerentes") { value(4) }
        }
        assertEquals(SeedGerentes.ALL.map { it.cpf }.toSet(), gerentes.findAll().map { it.cpf }.toSet())
        assertEquals("Geniéve", checkNotNull(gerentes.findByCpf("98574307084")).nome)
        assertTrue(gerentes.findAll().all { it.ativo })
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
    fun `lists active managers in pt-BR name order`() {
        mockMvc
            .get("/gerentes") { gerente("98574307084") }
            .andExpect {
                status { isOk() }
                jsonPath("$.gerentes[*].nome") {
                    value(contains("Gadamântio", "Geniéve", "Godophredo", "Gyândula"))
                }
                jsonPath("$.gerentes[0].quantidadeClientes") { value(null) }
                jsonPath("$._links.criacao.href") { value(containsString("/gerentes")) }
            }
    }

    @Test
    fun `put updates name and phone but not email`() {
        mockMvc
            .put("/gerentes/98574307084") {
                gerente("64065268052")
                contentType = MediaType.APPLICATION_JSON
                content = """{"nome":"Geniéve Silva","telefone":"41988889999"}"""
            }.andExpect {
                status { isOk() }
                jsonPath("$.nome") { value("Geniéve Silva") }
                jsonPath("$.telefone") { value("41988889999") }
                jsonPath("$.email") { value("ger1@bantads.com.br") }
            }
        mockMvc
            .put("/gerentes/98574307084") {
                gerente("64065268052")
                contentType = MediaType.APPLICATION_JSON
                content =
                    """{"nome":"Geniéve","telefone":"41988880001","email":"outro@bantads.com.br"}"""
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.status") { value(400) }
            }
        mockMvc
            .get("/gerentes/98574307084") { gerente("64065268052") }
            .andExpect {
                jsonPath("$.email") { value("ger1@bantads.com.br") }
                jsonPath("$.nome") { value("Geniéve Silva") }
            }
    }

    @Test
    fun `cannot inactivate the last active manager`() {
        inativar("98574307084", "saga-in-1")
        inativar("64065268052", "saga-in-2")
        inativar("23862179060", "saga-in-3")
        val last =
            commands.handle(
                MessageEnvelope(
                    sagaId = "saga-in-4",
                    tipo = CommandTypes.GERENTE_INATIVAR,
                    timestamp = "2026-04-30T10:00:00",
                    payload = mapOf("cpf" to "40501740066"),
                ),
            )
        assertEquals(ReplyStatus.FALHA, last.status)
        assertEquals(GerenteRules.ULTIMO_ATIVO, last.erro)
        mockMvc
            .get("/gerentes") { gerente("40501740066") }
            .andExpect {
                jsonPath("$.gerentes.length()") { value(1) }
                jsonPath("$.gerentes[0].cpf") { value("40501740066") }
            }
        mockMvc
            .get("/gerentes/98574307084") { gerente("40501740066") }
            .andExpect {
                jsonPath("$.ativo") { value(false) }
                jsonPath("$._links.atualizacao") { doesNotExist() }
                jsonPath("$._links.remocao") { doesNotExist() }
            }
    }

    @Test
    fun `insert duplicate email fails and compensation deletes`() {
        val created =
            commands.handle(
                MessageEnvelope(
                    sagaId = "saga-ins",
                    tipo = CommandTypes.GERENTE_INSERIR,
                    timestamp = "2026-04-30T10:00:00",
                    payload =
                        mapOf(
                            "cpf" to "11122233396",
                            "nome" to "Novo",
                            "email" to "novo@bantads.com.br",
                            "telefone" to "41988880009",
                        ),
                ),
            )
        assertEquals(ReplyStatus.SUCESSO, created.status)
        val dup =
            commands.handle(
                MessageEnvelope(
                    sagaId = "saga-dup",
                    tipo = CommandTypes.GERENTE_INSERIR,
                    timestamp = "2026-04-30T10:00:00",
                    payload =
                        mapOf(
                            "cpf" to "22233344405",
                            "nome" to "Outro",
                            "email" to "ger1@bantads.com.br",
                            "telefone" to "41988880010",
                        ),
                ),
            )
        assertEquals(ReplyStatus.FALHA, dup.status)
        assertEquals(GerenteRules.EMAIL_DUPLICADO, dup.erro)
        commands.handle(
            MessageEnvelope(
                sagaId = "saga-del",
                tipo = CommandTypes.GERENTE_REMOVER,
                timestamp = "2026-04-30T10:00:00",
                payload = mapOf("cpf" to "11122233396"),
            ),
        )
        mockMvc
            .get("/gerentes/11122233396") { gerente("98574307084") }
            .andExpect { status { isNotFound() } }
    }

    @Test
    fun `omits remocao link for the authenticated manager`() {
        mockMvc
            .get("/gerentes/98574307084") { gerente("98574307084") }
            .andExpect {
                jsonPath("$._links.self.href") { value(containsString("/gerentes/98574307084")) }
                jsonPath("$._links.remocao") { doesNotExist() }
                jsonPath("$._links.atualizacao.href") { value(containsString("/gerentes/98574307084")) }
            }
        mockMvc
            .get("/gerentes/64065268052") { gerente("98574307084") }
            .andExpect {
                jsonPath("$._links.remocao.href") { value(containsString("/gerentes/64065268052")) }
            }
    }

    @Test
    fun `cliente cannot list gerentes`() {
        mockMvc
            .get("/gerentes") {
                header("X-User-CPF", "12912861012")
                header("X-User-Tipo", Perfil.CLIENTE.wire)
            }.andExpect {
                status { isForbidden() }
                jsonPath("$.status") { value(403) }
            }
    }

    private fun inativar(
        cpf: String,
        sagaId: String,
    ) {
        val reply =
            commands.handle(
                MessageEnvelope(
                    sagaId = sagaId,
                    tipo = CommandTypes.GERENTE_INATIVAR,
                    timestamp = "2026-04-30T10:00:00",
                    payload = mapOf("cpf" to cpf),
                ),
            )
        assertEquals(ReplyStatus.SUCESSO, reply.status)
    }

    private fun MockHttpServletRequestDsl.gerente(cpf: String) {
        header("X-User-CPF", cpf)
        header("X-User-Tipo", Perfil.GERENTE.wire)
    }

    companion object {
        @Container
        @JvmField
        val postgres =
            PostgreSQLContainer(DockerImageName.parse("postgres:16"))
                .withDatabaseName("bantads")
                .withUsername("bantads")
                .withPassword("test")
                .withInitScript("postgres-init.sql")

        @JvmStatic
        @DynamicPropertySource
        fun datasource(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") {
                val base = postgres.jdbcUrl
                val sep = if (base.contains('?')) "&" else "?"
                base + sep + "currentSchema=gerente"
            }
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
        }
    }
}
