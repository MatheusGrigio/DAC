package br.ufpr.dac.bantads.conta

import br.ufpr.dac.bantads.conta.command.seed.SeedContas
import br.ufpr.dac.bantads.conta.command.store.EventStore
import br.ufpr.dac.bantads.conta.query.project.EventProjector
import br.ufpr.dac.bantads.conta.query.store.ContaReadRepository
import br.ufpr.dac.bantads.conta.saga.ContaCommandHandler
import br.ufpr.dac.bantads.shared.amqp.CommandTypes
import br.ufpr.dac.bantads.shared.amqp.MessageEnvelope
import br.ufpr.dac.bantads.shared.amqp.ReplyStatus
import br.ufpr.dac.bantads.shared.domain.Perfil
import br.ufpr.dac.bantads.shared.money.Money
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
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
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class ContaIT {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var store: EventStore

    @Autowired
    lateinit var projector: EventProjector

    @Autowired
    lateinit var contasQuery: ContaReadRepository

    @Autowired
    lateinit var commands: ContaCommandHandler

    @BeforeEach
    fun seed() {
        mockMvc.post("/internal/reboot").andExpect { status { isOk() } }
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
    fun `replay after reboot Catharyna is 800_00`() {
        assertEquals(Money.parse("800.00"), store.state("1291").saldo)
    }

    @Test
    fun `reboot twice restores the same seed and query matches command`() {
        mockMvc
            .post("/contas/1291/deposito") {
                cliente(SeedContas.CATHARYNA)
                contentType = MediaType.APPLICATION_JSON
                content = """{"valor":"10.00"}"""
            }.andExpect { status { isCreated() } }
        mockMvc.post("/internal/reboot").andExpect {
            status { isOk() }
            jsonPath("$.status") { value("ok") }
            jsonPath("$.contas") { value(5) }
            jsonPath("$.eventos") { value(SeedContas.EVENTS.size) }
            jsonPath("$._links") { doesNotExist() }
        }
        mockMvc.post("/internal/reboot").andExpect {
            status { isOk() }
            jsonPath("$.contas") { value(5) }
            jsonPath("$.eventos") { value(SeedContas.EVENTS.size) }
        }
        assertEquals(Money.parse("800.00"), store.state("1291").saldo)
        assertEquals(Money.parse("10000.00"), store.state("0950").saldo)
        assertEquals(Money.parse("200.00"), store.state("8573").saldo)
        assertEquals(Money.parse("150000.00"), store.state("5887").saldo)
        assertEquals(Money.parse("1500.00"), store.state("7617").saldo)
        val query = checkNotNull(contasQuery.findByCpfCliente(SeedContas.CATHARYNA))
        assertEquals(0, store.state("1291").saldo.compareTo(query.saldo))
        assertEquals("1291", query.numero)
        mockMvc.get("/contas/1291") { cliente(SeedContas.CATHARYNA) }.andExpect {
            jsonPath("$.saldo") { value("800.00") }
        }
    }

    @Test
    fun `deposit returns 201 without new balance`() {
        mockMvc
            .post("/contas/1291/deposito") {
                cliente(SeedContas.CATHARYNA)
                contentType = MediaType.APPLICATION_JSON
                content = """{"valor":"10.00"}"""
            }.andExpect {
                status { isCreated() }
                jsonPath("$.numeroConta") { value("1291") }
                jsonPath("$.tipo") { value("DEPOSITO") }
                jsonPath("$.valor") { value("10.00") }
                jsonPath("$.saldo") { doesNotExist() }
                jsonPath("$._links.conta.href") { value(containsString("/contas/1291")) }
            }
        assertEquals(Money.parse("810.00"), store.state("1291").saldo)
    }

    @Test
    fun `withdraw over balance is 422`() {
        mockMvc
            .post("/contas/1291/saque") {
                cliente(SeedContas.CATHARYNA)
                contentType = MediaType.APPLICATION_JSON
                content = """{"valor":"800.01"}"""
            }.andExpect {
                status { isUnprocessableEntity() }
                jsonPath("$.status") { value(422) }
                jsonPath("$.mensagem") { value("Saldo insuficiente para a operação") }
            }
        assertEquals(Money.parse("800.00"), store.state("1291").saldo)
    }

    @Test
    fun `gerente cannot withdraw`() {
        mockMvc
            .post("/contas/1291/saque") {
                header("X-User-CPF", SeedContas.GENIEVE)
                header("X-User-Tipo", Perfil.GERENTE.wire)
                contentType = MediaType.APPLICATION_JSON
                content = """{"valor":"10.00"}"""
            }.andExpect { status { isForbidden() } }
    }

    @Test
    fun `transfer is atomic and same account or missing dest is 422`() {
        val versaoAntes = store.state("1291").versao
        mockMvc
            .post("/contas/1291/transferencia") {
                cliente(SeedContas.CATHARYNA)
                contentType = MediaType.APPLICATION_JSON
                content = transferenciaJson("1291", "1291", SeedContas.CATHARYNA, SeedContas.CATHARYNA)
            }.andExpect { status { isUnprocessableEntity() } }
        mockMvc
            .post("/contas/1291/transferencia") {
                cliente(SeedContas.CATHARYNA)
                contentType = MediaType.APPLICATION_JSON
                content = transferenciaJson("1291", "0001", SeedContas.CATHARYNA, "00000000000")
            }.andExpect { status { isUnprocessableEntity() } }
        assertEquals(versaoAntes, store.state("1291").versao)
        mockMvc
            .post("/contas/1291/transferencia") {
                cliente(SeedContas.CATHARYNA)
                contentType = MediaType.APPLICATION_JSON
                content =
                    transferenciaJson(
                        "1291",
                        "0950",
                        SeedContas.CATHARYNA,
                        SeedContas.CLEUDDONIO,
                    )
            }.andExpect {
                status { isCreated() }
                jsonPath("$.tipo") { value("TRANSFERENCIA") }
                jsonPath("$.destino.numeroConta") { value("0950") }
            }
        assertEquals(Money.parse("700.00"), store.state("1291").saldo)
        assertEquals(Money.parse("10100.00"), store.state("0950").saldo)
    }

    @Test
    fun `concurrent withdrawals one succeeds one is 422`() {
        val pool = Executors.newFixedThreadPool(2)
        try {
            val tasks =
                listOf(
                    Callable { saqueStatus("800.00") },
                    Callable { saqueStatus("800.00") },
                )
            val codes = pool.invokeAll(tasks).map { it.get(15, TimeUnit.SECONDS) }
            assertTrue(codes.contains(201))
            assertTrue(codes.contains(422))
            assertEquals(Money.parse("0.00"), store.state("1291").saldo)
        } finally {
            pool.shutdownNow()
        }
    }

    @Test
    fun `R13 after seed points to 7617`() {
        val reply =
            commands.handle(
                MessageEnvelope(
                    sagaId = "saga-r13",
                    tipo = CommandTypes.CONTA_IDENTIFICAR_CONTA_PARA_NOVO_GERENTE,
                    timestamp = "2026-04-30T10:00:00",
                    payload =
                        mapOf(
                            "gerentes" to
                                listOf(
                                    SeedContas.GENIEVE,
                                    SeedContas.GODOPHREDO,
                                    SeedContas.GYANDULA,
                                    "40501740066",
                                ),
                        ),
                ),
            )
        assertEquals(ReplyStatus.SUCESSO, reply.status)
        assertEquals("7617", reply.payload["numero"])
        assertEquals(false, reply.payload["semConta"])
    }

    @Test
    fun `criar account number is not cpf prefix`() {
        val reply =
            commands.handle(
                MessageEnvelope(
                    sagaId = "saga-criar",
                    tipo = CommandTypes.CONTA_CRIAR,
                    timestamp = "2026-04-30T10:00:00",
                    payload = mapOf("cpfCliente" to "11122233396", "cpfGerente" to SeedContas.GENIEVE),
                ),
            )
        assertEquals(ReplyStatus.SUCESSO, reply.status)
        val numero = reply.payload["numero"] as String
        assertEquals(4, numero.length)
        assertNotEquals("1112", numero)
        assertTrue(store.state(numero).existe)
    }

    @Test
    fun `query seed Catharyna is 800_00 with owner links`() {
        mockMvc
            .get("/contas/1291") { cliente(SeedContas.CATHARYNA) }
            .andExpect {
                status { isOk() }
                jsonPath("$.numero") { value("1291") }
                jsonPath("$.saldo") { value("800.00") }
                jsonPath("$.cpfCliente") { value(SeedContas.CATHARYNA) }
                jsonPath("$.dataCriacao") { value("2000-01-01") }
                jsonPath("$._links.self.href") { value(containsString("/contas/1291")) }
                jsonPath("$._links.cliente.href") { value(containsString("/clientes/${SeedContas.CATHARYNA}")) }
                jsonPath("$._links.deposito.href") { value(containsString("/deposito")) }
                jsonPath("$._links.extrato.href") { value(containsString("/extrato")) }
            }
        mockMvc
            .get("/clientes/${SeedContas.CATHARYNA}/conta") { cliente(SeedContas.CATHARYNA) }
            .andExpect {
                status { isOk() }
                jsonPath("$.saldo") { value("800.00") }
            }
    }

    @Test
    fun `gerente sees account without write links and other client is 403`() {
        mockMvc
            .get("/contas/1291") { gerente() }
            .andExpect {
                status { isOk() }
                jsonPath("$.saldo") { value("800.00") }
                jsonPath("$._links.deposito") { doesNotExist() }
                jsonPath("$._links.saque") { doesNotExist() }
                jsonPath("$._links.extrato") { doesNotExist() }
                jsonPath("$._links.cliente.href") { value(containsString("/clientes/")) }
            }
        mockMvc.get("/contas/1291") { cliente(SeedContas.CLEUDDONIO) }.andExpect { status { isForbidden() } }
        mockMvc.get("/contas/1291/extrato") { gerente() }.andExpect { status { isForbidden() } }
    }

    @Test
    fun `extrato january 2020 has opening zero and seven movements`() {
        mockMvc
            .get("/contas/1291/extrato") {
                cliente(SeedContas.CATHARYNA)
                param("inicio", "2020-01-01")
                param("fim", "2020-01-31")
            }.andExpect {
                status { isOk() }
                jsonPath("$.saldoAbertura") { value("0.00") }
                jsonPath("$.dataInicio") { value("2020-01-01") }
                jsonPath("$.dataFim") { value("2020-01-31") }
                jsonPath("$.movimentacoes.length()") { value(7) }
                jsonPath("$.movimentacoes[6].tipo") { value("TRANSFERENCIA") }
                jsonPath("$.movimentacoes[6].origem.nome") { value("Catharyna") }
                jsonPath("$.movimentacoes[6].destino.numeroConta") { value("0950") }
                jsonPath("$._links.conta.href") { value(containsString("/contas/1291")) }
            }
        mockMvc
            .get("/contas/1291/extrato") {
                cliente(SeedContas.CATHARYNA)
                param("inicio", "2020-01-01")
                param("fim", "2021-01-02")
            }.andExpect { status { isUnprocessableEntity() } }
        mockMvc
            .get("/contas/1291/extrato") {
                cliente(SeedContas.CATHARYNA)
                param("inicio", "2020-02-01")
                param("fim", "2020-01-01")
            }.andExpect { status { isUnprocessableEntity() } }
    }

    @Test
    fun `duplicate projection does not double balance`() {
        projector.applyAll(store.load("1291"))
        mockMvc.get("/contas/1291") { cliente(SeedContas.CATHARYNA) }.andExpect {
            status { isOk() }
            jsonPath("$.saldo") { value("800.00") }
        }
    }

    @Test
    fun `query follows deposit after projection`() {
        mockMvc
            .post("/contas/1291/deposito") {
                cliente(SeedContas.CATHARYNA)
                contentType = MediaType.APPLICATION_JSON
                content = """{"valor":"10.00"}"""
            }.andExpect { status { isCreated() } }
        projector.applyAll(store.load("1291"))
        mockMvc.get("/contas/1291") { cliente(SeedContas.CATHARYNA) }.andExpect {
            jsonPath("$.saldo") { value("810.00") }
        }
    }

    @Test
    fun `internal composition maps seed saldos and manager counts`() {
        mockMvc.get("/internal/saldos") { gerente() }.andExpect {
            status { isOk() }
            jsonPath("$['12912861012'].saldo") { value("800.00") }
            jsonPath("$['12912861012'].numero") { value("1291") }
            jsonPath("$['09506382000'].saldo") { value("10000.00") }
        }
        mockMvc.get("/internal/contagem-por-gerente") { gerente() }.andExpect {
            status { isOk() }
            jsonPath("$['98574307084']") { value(2) }
            jsonPath("$['64065268052']") { value(2) }
            jsonPath("$['23862179060']") { value(1) }
        }
        mockMvc.get("/internal/contas/0950") { cliente(SeedContas.CATHARYNA) }.andExpect {
            status { isOk() }
            jsonPath("$.numero") { value("0950") }
            jsonPath("$.cpfCliente") { value(SeedContas.CLEUDDONIO) }
        }
        mockMvc.get("/internal/contas/0001") { cliente(SeedContas.CATHARYNA) }.andExpect { status { isNotFound() } }
    }

    private fun saqueStatus(valor: String): Int =
        mockMvc
            .post("/contas/1291/saque") {
                cliente(SeedContas.CATHARYNA)
                contentType = MediaType.APPLICATION_JSON
                content = """{"valor":"$valor"}"""
            }.andReturn()
            .response.status

    private fun MockHttpServletRequestDsl.cliente(cpf: String) {
        header("X-User-CPF", cpf)
        header("X-User-Tipo", Perfil.CLIENTE.wire)
    }

    private fun MockHttpServletRequestDsl.gerente() {
        header("X-User-CPF", SeedContas.GENIEVE)
        header("X-User-Tipo", Perfil.GERENTE.wire)
    }

    companion object {
        fun transferenciaJson(
            origem: String,
            destino: String,
            cpfOrigem: String,
            cpfDestino: String,
        ): String =
            """
            {"valor":"100.00",
            "origem":{"numeroConta":"$origem","cpf":"$cpfOrigem","nome":"Catharyna"},
            "destino":{"numeroConta":"$destino","cpf":"$cpfDestino","nome":"Destino"}}
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
                base + sep + "currentSchema=conta_command"
            }
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("query.datasource.url") {
                val base = postgres.jdbcUrl
                val sep = if (base.contains('?')) "&" else "?"
                base + sep + "currentSchema=conta_query"
            }
            registry.add("query.datasource.username", postgres::getUsername)
            registry.add("query.datasource.password", postgres::getPassword)
        }
    }
}
