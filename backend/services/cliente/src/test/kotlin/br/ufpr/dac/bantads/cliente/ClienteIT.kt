package br.ufpr.dac.bantads.cliente

import br.ufpr.dac.bantads.cliente.cadastro.ClienteRepository
import br.ufpr.dac.bantads.cliente.saga.ClienteCommandHandler
import br.ufpr.dac.bantads.cliente.seed.SeedClientes
import br.ufpr.dac.bantads.cliente.solicitacao.SolicitacaoRepository
import br.ufpr.dac.bantads.shared.amqp.CommandTypes
import br.ufpr.dac.bantads.shared.amqp.MessageEnvelope
import br.ufpr.dac.bantads.shared.amqp.ReplyStatus
import br.ufpr.dac.bantads.shared.domain.Perfil
import org.hamcrest.Matchers.containsInAnyOrder
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
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
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class ClienteIT {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var clientes: ClienteRepository

    @Autowired
    lateinit var solicitacoes: SolicitacaoRepository

    @Autowired
    lateinit var commands: ClienteCommandHandler

    @BeforeEach
    fun seed() {
        mockMvc.post("/internal/reboot").andExpect { status { isOk() } }
    }

    @Test
    fun `reboot twice restores five clientes and clears solicitacoes`() {
        mockMvc
            .post("/solicitacoes") {
                contentType = MediaType.APPLICATION_JSON
                content = NOVA_SOLICITACAO
            }.andExpect { status { isCreated() } }
        mockMvc.post("/internal/reboot").andExpect {
            status { isOk() }
            jsonPath("$.status") { value("ok") }
            jsonPath("$.clientes") { value(5) }
            jsonPath("$._links") { doesNotExist() }
        }
        mockMvc.post("/internal/reboot").andExpect {
            status { isOk() }
            jsonPath("$.clientes") { value(5) }
        }
        assertEquals(SeedClientes.ALL.map { it.cpf }.toSet(), clientes.findAll().map { it.cpf }.toSet())
        assertEquals(0, solicitacoes.count())
        val catharyna = checkNotNull(clientes.findByCpf("12912861012"))
        assertEquals("Catharyna", catharyna.nome)
        assertEquals("Curitiba", catharyna.endereco.cidade)
        assertEquals("PR", catharyna.endereco.uf)
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
    fun `autocadastro returns 201 location and pending hateoas`() {
        mockMvc
            .post("/solicitacoes") {
                contentType = MediaType.APPLICATION_JSON
                content = NOVA_SOLICITACAO
            }.andExpect {
                status { isCreated() }
                header { string("Location", "/solicitacoes/11122233396") }
                jsonPath("$.cpf") { value("11122233396") }
                jsonPath("$.salario") { value("4500.00") }
                jsonPath("$.status") { value("PENDENTE") }
                jsonPath("$.motivo") { value(null) }
                jsonPath("$._links.self.href") { value(containsString("/solicitacoes/11122233396")) }
                jsonPath("$._links.aprovacao.href") { value(containsString("/aprovacao")) }
                jsonPath("$._links.rejeicao.href") { value(containsString("/rejeicao")) }
            }
    }

    @Test
    fun `duplicate cpf or email is 409`() {
        mockMvc
            .post("/solicitacoes") {
                contentType = MediaType.APPLICATION_JSON
                content = NOVA_SOLICITACAO
            }.andExpect { status { isCreated() } }
        mockMvc
            .post("/solicitacoes") {
                contentType = MediaType.APPLICATION_JSON
                content = NOVA_SOLICITACAO
            }.andExpect {
                status { isConflict() }
                jsonPath("$.status") { value(409) }
                jsonPath("$.erro") { value("Conflict") }
            }
        mockMvc
            .post("/solicitacoes") {
                contentType = MediaType.APPLICATION_JSON
                content = solicitacaoJson("22233344405", "Outro", "fulano@exemplo.com.br")
            }.andExpect { status { isConflict() } }
        mockMvc
            .post("/solicitacoes") {
                contentType = MediaType.APPLICATION_JSON
                content = solicitacaoJson("12912861012", "Catharyna 2", "outra@exemplo.com.br")
            }.andExpect { status { isConflict() } }
        mockMvc
            .post("/solicitacoes") {
                contentType = MediaType.APPLICATION_JSON
                content = solicitacaoJson("22233344405", "Outro", "cli1@bantads.com.br")
            }.andExpect { status { isConflict() } }
    }

    @Test
    fun `gerente lists solicitacoes and cliente cannot`() {
        mockMvc
            .post("/solicitacoes") {
                contentType = MediaType.APPLICATION_JSON
                content = NOVA_SOLICITACAO
            }.andExpect { status { isCreated() } }
        mockMvc
            .get("/solicitacoes") { gerente() }
            .andExpect {
                status { isOk() }
                jsonPath("$.solicitacoes[0].cpf") { value("11122233396") }
                jsonPath("$._links.self.href") { value(containsString("/solicitacoes")) }
            }
        mockMvc
            .get("/solicitacoes") { cliente("12912861012") }
            .andExpect {
                status { isForbidden() }
                jsonPath("$.status") { value(403) }
            }
    }

    @Test
    fun `rejeicao is 200 then 409 when not pending`() {
        mockMvc
            .post("/solicitacoes") {
                contentType = MediaType.APPLICATION_JSON
                content = NOVA_SOLICITACAO
            }.andExpect { status { isCreated() } }
        mockMvc
            .post("/solicitacoes/11122233396/rejeicao") {
                gerente()
                contentType = MediaType.APPLICATION_JSON
                content = """{"motivo":"Renda incompatível com a política do banco"}"""
            }.andExpect {
                status { isOk() }
                jsonPath("$.status") { value("NAO_APROVADA") }
                jsonPath("$.motivo") { value("Renda incompatível com a política do banco") }
                jsonPath("$._links.aprovacao") { doesNotExist() }
                jsonPath("$._links.rejeicao") { doesNotExist() }
            }
        mockMvc
            .post("/solicitacoes/11122233396/rejeicao") {
                gerente()
                contentType = MediaType.APPLICATION_JSON
                content = """{"motivo":"outra"}"""
            }.andExpect { status { isConflict() } }
    }

    @Test
    fun `busca Cat finds Catharyna and Catianna`() {
        mockMvc
            .get("/clientes") {
                gerente()
                param("busca", "Cat")
            }.andExpect {
                status { isOk() }
                jsonPath("$.clientes[*].nome") {
                    value(containsInAnyOrder("Catharyna", "Catianna"))
                }
                jsonPath("$.clientes[*].nome") { value(not(hasItem("Cleuddônio"))) }
            }
    }

    @Test
    fun `cliente can read own cadastro but not another`() {
        mockMvc
            .get("/clientes/12912861012") { cliente("12912861012") }
            .andExpect {
                status { isOk() }
                jsonPath("$.cpf") { value("12912861012") }
                jsonPath("$.salario") { value("10000.00") }
                jsonPath("$._links.conta.href") { value(containsString("/clientes/12912861012/conta")) }
            }
        mockMvc
            .get("/clientes/09506382000") { cliente("12912861012") }
            .andExpect { status { isForbidden() } }
    }

    @Test
    fun `marcar aprovada criar and idempotent inbox`() {
        mockMvc
            .post("/solicitacoes") {
                contentType = MediaType.APPLICATION_JSON
                content = NOVA_SOLICITACAO
            }.andExpect { status { isCreated() } }
        val envelope =
            MessageEnvelope(
                sagaId = "saga-aprovar",
                tipo = CommandTypes.CLIENTE_MARCAR_APROVADA,
                timestamp = "2026-04-30T10:00:00",
                payload = mapOf("cpf" to "11122233396"),
            )
        val first = commands.handle(envelope)
        val second = commands.handle(envelope)
        assertEquals(ReplyStatus.SUCESSO, first.status)
        assertEquals(first.payload["email"], second.payload["email"])
        val criar =
            commands.handle(
                MessageEnvelope(
                    sagaId = "saga-criar",
                    tipo = CommandTypes.CLIENTE_CRIAR,
                    timestamp = "2026-04-30T10:00:00",
                    payload = mapOf("cpf" to "11122233396"),
                ),
            )
        assertEquals(ReplyStatus.SUCESSO, criar.status)
        assertEquals("11122233396", clientes.findByCpf("11122233396")?.cpf)
        val notPending =
            commands.handle(
                MessageEnvelope(
                    sagaId = "saga-again",
                    tipo = CommandTypes.CLIENTE_MARCAR_APROVADA,
                    timestamp = "2026-04-30T10:00:00",
                    payload = mapOf("cpf" to "11122233396"),
                ),
            )
        assertEquals(ReplyStatus.FALHA, notPending.status)
        assertEquals("Solicitação não está PENDENTE", notPending.erro)
    }

    @Test
    fun `desmarcar aprovada returns to PENDENTE but not NAO_APROVADA`() {
        mockMvc
            .post("/solicitacoes") {
                contentType = MediaType.APPLICATION_JSON
                content = NOVA_SOLICITACAO
            }.andExpect { status { isCreated() } }
        commands.handle(
            MessageEnvelope(
                sagaId = "saga-ok",
                tipo = CommandTypes.CLIENTE_MARCAR_APROVADA,
                timestamp = "2026-04-30T10:00:00",
                payload = mapOf("cpf" to "11122233396"),
            ),
        )
        val undone =
            commands.handle(
                MessageEnvelope(
                    sagaId = "saga-undo",
                    tipo = CommandTypes.CLIENTE_DESMARCAR_APROVADA,
                    timestamp = "2026-04-30T10:00:00",
                    payload = mapOf("cpf" to "11122233396"),
                ),
            )
        assertEquals(ReplyStatus.SUCESSO, undone.status)
        mockMvc
            .get("/solicitacoes/11122233396") { gerente() }
            .andExpect {
                jsonPath("$.status") { value("PENDENTE") }
                jsonPath("$.motivo") { value(null) }
            }
        commands.handle(
            MessageEnvelope(
                sagaId = "saga-dup",
                tipo = CommandTypes.CLIENTE_MARCAR_NAO_APROVADA,
                timestamp = "2026-04-30T10:00:00",
                payload = mapOf("cpf" to "11122233396"),
            ),
        )
        commands.handle(
            MessageEnvelope(
                sagaId = "saga-undo-2",
                tipo = CommandTypes.CLIENTE_DESMARCAR_APROVADA,
                timestamp = "2026-04-30T10:00:00",
                payload = mapOf("cpf" to "11122233396"),
            ),
        )
        mockMvc
            .get("/solicitacoes/11122233396") { gerente() }
            .andExpect {
                jsonPath("$.status") { value("NAO_APROVADA") }
                jsonPath("$.motivo") { value("E-mail já cadastrado") }
            }
        assertNull(
            commands
                .handle(
                    MessageEnvelope(
                        sagaId = "saga-obter",
                        tipo = CommandTypes.CLIENTE_OBTER_POR_CPFS,
                        timestamp = "2026-04-30T10:00:00",
                        payload = mapOf("cpfs" to listOf("12912861012")),
                    ),
                ).erro,
        )
    }

    @Test
    fun `malformed autocadastro is 400`() {
        mockMvc
            .post("/solicitacoes") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"cpf":"123","nome":"X"}"""
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.status") { value(400) }
            }
    }

    private fun MockHttpServletRequestDsl.gerente() {
        header("X-User-CPF", "98574307084")
        header("X-User-Tipo", Perfil.GERENTE.wire)
    }

    private fun MockHttpServletRequestDsl.cliente(cpf: String) {
        header("X-User-CPF", cpf)
        header("X-User-Tipo", Perfil.CLIENTE.wire)
    }

    companion object {
        val NOVA_SOLICITACAO =
            """
            {"cpf":"11122233396","nome":"Fulano de Tal","email":"fulano@exemplo.com.br",
            "telefone":"41999990000","salario":"4500.00",
            "endereco":{"logradouro":"Rua XV de Novembro","numero":"1299","complemento":null,
            "cep":"80060000","cidade":"Curitiba","uf":"PR"}}
            """.trimIndent().replace("\n", "")

        fun solicitacaoJson(
            cpf: String,
            nome: String,
            email: String,
        ): String =
            """
            {"cpf":"$cpf","nome":"$nome","email":"$email","telefone":"41999990000","salario":"4500.00",
            "endereco":{"logradouro":"Rua XV de Novembro","numero":"1299","cep":"80060000","cidade":"Curitiba","uf":"PR"}}
            """.trimIndent().replace("\n", "")

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
                base + sep + "currentSchema=cliente"
            }
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
        }
    }
}
