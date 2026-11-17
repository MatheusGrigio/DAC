package br.ufpr.dac.bantads.saga

import br.ufpr.dac.bantads.saga.engine.SagaState
import br.ufpr.dac.bantads.saga.engine.SagaStatuses
import br.ufpr.dac.bantads.saga.job.JobRecord
import br.ufpr.dac.bantads.saga.store.JobStore
import br.ufpr.dac.bantads.saga.store.SagaStateStore
import br.ufpr.dac.bantads.shared.amqp.CommandTypes
import br.ufpr.dac.bantads.shared.amqp.MessageEnvelope
import br.ufpr.dac.bantads.shared.amqp.QueueNames
import br.ufpr.dac.bantads.shared.amqp.ReplyEnvelope
import br.ufpr.dac.bantads.shared.amqp.ReplyStatus
import br.ufpr.dac.bantads.shared.domain.JobStatus
import br.ufpr.dac.bantads.shared.json.BantadsJson
import br.ufpr.dac.bantads.shared.time.DateTimes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.amqp.core.AmqpAdmin
import org.springframework.amqp.core.Message
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.RabbitMQContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Import(TestAmqpQueues::class)
class SagaIT {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var rabbitTemplate: RabbitTemplate

    @Autowired
    lateinit var connectionFactory: ConnectionFactory

    @Autowired
    lateinit var amqpAdmin: AmqpAdmin

    @Autowired
    lateinit var jobs: JobStore

    @Autowired
    lateinit var sagas: SagaStateStore

    private val mapper = BantadsJson.mapper()

    @BeforeEach
    fun purge() {
        listOf(
            QueueNames.SAGA_CMD,
            QueueNames.ORQUESTRADOR_REPLY,
            QueueNames.MS_AUTH_CMD,
            QueueNames.MS_AUTH_CMD_DLQ,
            QueueNames.MS_CLIENTE_CMD_DLQ,
            QueueNames.MS_GERENTE_CMD_DLQ,
            QueueNames.MS_CONTA_CMD_DLQ,
        ).forEach { amqpAdmin.purgeQueue(it, true) }
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
    fun `echo ping replies pong on the job`() {
        val sagaId = UUID.randomUUID().toString()
        val container = pingReplier()
        container.start()
        try {
            publishEcho(sagaId)
            val job = awaitJob(sagaId) { it.status == JobStatus.CONCLUIDO.wire }
            assertEquals("pong", job.resultado?.get("mensagem"))
            assertEquals(SagaStatuses.CONCLUIDO, awaitSaga(sagaId).status)
        } finally {
            container.stop()
        }
    }

    @Test
    fun `timeout with dead consumer compensates once even after DLQ`() {
        val sagaId = UUID.randomUUID().toString()
        publishEcho(sagaId)
        val failed = awaitJob(sagaId, timeoutMs = 8_000) { it.status == JobStatus.FALHA.wire }
        assertEquals("timeout", failed.erro)
        assertEquals(1, awaitSaga(sagaId).compensacoes)

        rabbitTemplate.convertAndSend(
            QueueNames.MS_AUTH_CMD_DLQ,
            mapper.writeValueAsString(
                MessageEnvelope(
                    sagaId = sagaId,
                    tipo = CommandTypes.ECHO_PING,
                    timestamp = DateTimes.now(),
                ),
            ),
        )
        Thread.sleep(400)
        assertEquals(1, sagas.find(sagaId)!!.compensacoes)
        assertEquals(JobStatus.FALHA.wire, jobs.find(sagaId)!!.status)
    }

    private fun publishEcho(sagaId: String) {
        rabbitTemplate.convertAndSend(
            QueueNames.SAGA_CMD,
            mapper.writeValueAsString(
                MessageEnvelope(
                    sagaId = sagaId,
                    tipo = CommandTypes.ECHO,
                    timestamp = DateTimes.now(),
                    payload = mapOf("nome" to "Echo"),
                ),
            ),
        )
    }

    private fun pingReplier(): SimpleMessageListenerContainer {
        val container = SimpleMessageListenerContainer(connectionFactory)
        container.setQueueNames(QueueNames.MS_AUTH_CMD)
        container.setMessageListener { message: Message ->
            val envelope: MessageEnvelope = mapper.readValue(String(message.body), MessageEnvelope::class.java)
            val sagaId = envelope.sagaId ?: return@setMessageListener
            if (envelope.tipo == CommandTypes.ECHO_PING) {
                rabbitTemplate.convertAndSend(
                    QueueNames.ORQUESTRADOR_REPLY,
                    mapper.writeValueAsString(
                        ReplyEnvelope(
                            sagaId = sagaId,
                            tipo = envelope.tipo,
                            timestamp = DateTimes.now(),
                            status = ReplyStatus.SUCESSO,
                        ),
                    ),
                )
            }
        }
        container.afterPropertiesSet()
        return container
    }

    private fun awaitJob(
        sagaId: String,
        timeoutMs: Long = 5_000,
        predicate: (JobRecord) -> Boolean,
    ): JobRecord {
        val end = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < end) {
            val job = jobs.find(sagaId)
            if (job != null && predicate(job)) {
                return job
            }
            Thread.sleep(50)
        }
        error("job $sagaId not in expected state: ${jobs.find(sagaId)}")
    }

    private fun awaitSaga(sagaId: String): SagaState {
        val end = System.currentTimeMillis() + 5_000
        while (System.currentTimeMillis() < end) {
            val state = sagas.find(sagaId)
            if (state != null) {
                return state
            }
            Thread.sleep(50)
        }
        error("saga $sagaId not stored")
    }

    companion object {
        @Container
        @JvmField
        val redis =
            GenericContainer(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379)

        @Container
        @JvmField
        val rabbit = RabbitMQContainer(DockerImageName.parse("rabbitmq:3.13-management"))

        @JvmStatic
        @DynamicPropertySource
        fun infra(registry: DynamicPropertyRegistry) {
            registry.add("spring.data.redis.host", redis::getHost)
            registry.add("spring.data.redis.port") { redis.getMappedPort(6379).toString() }
            registry.add("spring.rabbitmq.host", rabbit::getHost)
            registry.add("spring.rabbitmq.port") { rabbit.amqpPort.toString() }
            registry.add("spring.rabbitmq.username", rabbit::getAdminUsername)
            registry.add("spring.rabbitmq.password", rabbit::getAdminPassword)
            registry.add("saga.step-timeout") { "2s" }
            registry.add("saga.timeout-scan-ms") { "100" }
        }
    }
}
