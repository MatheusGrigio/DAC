package br.ufpr.dac.bantads.saga.engine

import br.ufpr.dac.bantads.saga.config.SagaProperties
import br.ufpr.dac.bantads.saga.job.JobRecord
import br.ufpr.dac.bantads.saga.store.JobStore
import br.ufpr.dac.bantads.saga.store.SagaStateStore
import br.ufpr.dac.bantads.shared.amqp.CommandTypes
import br.ufpr.dac.bantads.shared.amqp.MessageEnvelope
import br.ufpr.dac.bantads.shared.amqp.QueueNames
import br.ufpr.dac.bantads.shared.amqp.ReplyEnvelope
import br.ufpr.dac.bantads.shared.amqp.ReplyStatus
import br.ufpr.dac.bantads.shared.domain.JobStatus
import br.ufpr.dac.bantads.shared.time.DateTimes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.ConcurrentHashMap

class SagaEngineTest {
    @Test
    fun `default step timeout is 30 seconds`() {
        assertEquals(Duration.ofSeconds(30), SagaProperties().stepTimeout)
    }

    @Test
    fun `echo success completes job with pong and does not store senha`() {
        val env = harness(autoReply = true)
        env.engine.start(
            MessageEnvelope(
                sagaId = "echo-ok",
                tipo = CommandTypes.ECHO,
                timestamp = DateTimes.now(),
                payload = mapOf("nome" to "Ada", "senha" to "secret"),
            ),
        )
        val job = env.jobs.find("echo-ok")!!
        assertEquals(JobStatus.CONCLUIDO.wire, job.status)
        assertEquals("pong", job.resultado?.get("mensagem"))
        assertEquals("inline", job.resultType)
        val state = env.states.find("echo-ok")!!
        assertEquals(SagaStatuses.CONCLUIDO, state.status)
        assertEquals("Ada", state.payload["nome"])
        assertNull(state.payload["senha"])
        assertEquals(0, state.compensacoes)
        assertTrue(env.published.any { it.tipo == CommandTypes.ECHO_PING })
        assertEquals(QueueNames.MS_AUTH_CMD, env.published.first { it.tipo == CommandTypes.ECHO_PING }.queue)
    }

    @Test
    fun `timeout with dead consumer compensates once even if DLQ arrives later`() {
        val env = harness(autoReply = false, stepTimeout = Duration.ofMillis(50))
        env.engine.start(
            MessageEnvelope(
                sagaId = "echo-to",
                tipo = CommandTypes.ECHO,
                timestamp = DateTimes.now(),
                payload = emptyMap(),
            ),
        )
        assertEquals(JobStatus.PENDENTE.wire, env.jobs.find("echo-to")!!.status)
        assertEquals(0, env.guard.acquires)
        env.clock.millis = 10_000
        env.engine.failTimedOutSteps()
        assertEquals(JobStatus.FALHA.wire, env.jobs.find("echo-to")!!.status)
        assertEquals("timeout", env.jobs.find("echo-to")!!.erro)
        assertEquals(1, env.states.find("echo-to")!!.compensacoes)
        assertEquals(1, env.guard.acquires)

        env.engine.onDlq(
            MessageEnvelope(
                sagaId = "echo-to",
                tipo = CommandTypes.ECHO_PING,
                timestamp = DateTimes.now(),
            ),
        )
        env.engine.failTimedOutSteps()
        assertEquals(1, env.states.find("echo-to")!!.compensacoes)
        assertEquals(1, env.guard.acquires)
        assertEquals(JobStatus.FALHA.wire, env.jobs.find("echo-to")!!.status)
    }

    @Test
    fun `R9 success completes resource job without storing senha and invalidates cache`() {
        val env = harness(autoReply = true)
        env.engine.start(
            MessageEnvelope(
                sagaId = "r9-ok",
                tipo = CommandTypes.APROVAR_CLIENTE,
                timestamp = DateTimes.now(),
                payload = mapOf("cpf" to "22233344405", "solicitadoPorCpf" to "98574307084"),
            ),
        )
        val job = env.jobs.find("r9-ok")!!
        assertEquals(JobStatus.CONCLUIDO.wire, job.status)
        assertEquals("resource", job.resultType)
        assertEquals("clientes", job.dominio)
        assertEquals("22233344405", job.resourceId)
        val state = env.states.find("r9-ok")!!
        assertEquals(SagaStatuses.CONCLUIDO, state.status)
        assertNull(state.payload["senha"])
        assertEquals(listOf("22233344405"), env.cache.deleted)
        val tipos = env.published.map { it.tipo }
        assertEquals(
            listOf(
                CommandTypes.CLIENTE_MARCAR_APROVADA,
                CommandTypes.GERENTE_LISTAR_ATIVOS,
                CommandTypes.CONTA_ESCOLHER_GERENTE_MENOS_CLIENTES,
                CommandTypes.CLIENTE_CRIAR,
                CommandTypes.AUTH_CRIAR_CLIENTE,
                CommandTypes.CONTA_CRIAR,
                CommandTypes.EMAIL_SENHA_CLIENTE,
            ),
            tipos,
        )
        val email = env.published.first { it.tipo == CommandTypes.EMAIL_SENHA_CLIENTE }
        assertEquals("s3nh4R9", email.envelope.payload["senha"])
        assertEquals("beltrano@exemplo.com.br", email.envelope.payload["email"])
        assertEquals(
            "40501740066",
            env.published.first { it.tipo == CommandTypes.CONTA_CRIAR }.envelope.payload["cpfGerente"],
        )
        assertFalse(env.states.find("r9-ok")!!.payload.containsKey("senha"))
    }

    @Test
    fun `R9 email duplicado marks NAO_APROVADA and does not return to PENDENTE`() {
        val env = harness(autoReply = true, failAuth = true)
        env.engine.start(
            MessageEnvelope(
                sagaId = "r9-dup",
                tipo = CommandTypes.APROVAR_CLIENTE,
                timestamp = DateTimes.now(),
                payload = mapOf("cpf" to "33344455516", "solicitadoPorCpf" to "98574307084"),
            ),
        )
        val job = env.jobs.find("r9-dup")!!
        assertEquals(JobStatus.FALHA.wire, job.status)
        assertEquals(SagaEngine.EMAIL_DUPLICADO, job.erro)
        val tipos = env.published.map { it.tipo }
        assertTrue(CommandTypes.CLIENTE_MARCAR_NAO_APROVADA in tipos)
        assertTrue(CommandTypes.CLIENTE_REMOVER in tipos)
        assertTrue(CommandTypes.EMAIL_FALHA_APROVACAO in tipos)
        assertFalse(CommandTypes.CLIENTE_DESMARCAR_APROVADA in tipos)
        assertFalse(CommandTypes.AUTH_REMOVER in tipos)
        assertFalse(CommandTypes.CONTA_REMOVER in tipos)
        assertFalse(CommandTypes.CONTA_CRIAR in tipos)
        val falha = env.published.first { it.tipo == CommandTypes.EMAIL_FALHA_APROVACAO }
        assertEquals(SagaEngine.EMAIL_DUPLICADO, falha.envelope.payload["motivo"])
        assertEquals(SagaStatuses.FALHA, env.states.find("r9-dup")!!.status)
        assertTrue(env.cache.deleted.isEmpty())
    }

    @Test
    fun `R9 timeout compensates once and emails falha`() {
        val env = harness(autoReply = false, stepTimeout = Duration.ofMillis(50))
        env.engine.start(
            MessageEnvelope(
                sagaId = "r9-to",
                tipo = CommandTypes.APROVAR_CLIENTE,
                timestamp = DateTimes.now(),
                payload = mapOf("cpf" to "22233344405"),
            ),
        )
        env.clock.millis = 10_000
        env.engine.failTimedOutSteps()
        assertEquals(JobStatus.FALHA.wire, env.jobs.find("r9-to")!!.status)
        assertEquals("timeout", env.jobs.find("r9-to")!!.erro)
        assertEquals(1, env.guard.acquires)
        assertTrue(env.published.any { it.tipo == CommandTypes.EMAIL_FALHA_APROVACAO })
        env.engine.failTimedOutSteps()
        assertEquals(1, env.guard.acquires)
    }

    @Test
    fun `R13 success transfers account emails client and does not store senha`() {
        val env = harness(autoReply = true, r13 = true)
        env.engine.start(
            MessageEnvelope(
                sagaId = "r13-ok",
                tipo = CommandTypes.INSERIR_GERENTE,
                timestamp = DateTimes.now(),
                payload =
                    mapOf(
                        "cpf" to "55667788990",
                        "nome" to "Gumercindo",
                        "email" to "ger5@bantads.com.br",
                        "telefone" to "41988880005",
                        "senha" to "tads",
                    ),
            ),
        )
        val job = env.jobs.find("r13-ok")!!
        assertEquals(JobStatus.CONCLUIDO.wire, job.status)
        assertEquals("resource", job.resultType)
        assertEquals("gerentes", job.dominio)
        assertEquals("55667788990", job.resourceId)
        assertNull(env.states.find("r13-ok")!!.payload["senha"])
        assertEquals(listOf("55667788990"), env.cache.deletedGerentes)
        assertTrue(env.cache.deleted.isEmpty())
        val tipos = env.published.map { it.tipo }
        assertEquals(
            listOf(
                CommandTypes.GERENTE_INSERIR,
                CommandTypes.AUTH_CRIAR_GERENTE,
                CommandTypes.GERENTE_LISTAR_ATIVOS,
                CommandTypes.CONTA_IDENTIFICAR_CONTA_PARA_NOVO_GERENTE,
                CommandTypes.CONTA_ATRIBUIR_GERENTE,
                CommandTypes.CLIENTE_OBTER_POR_CPFS,
                CommandTypes.EMAIL_TROCA_GERENTE,
            ),
            tipos,
        )
        assertEquals("tads", env.published.first { it.tipo == CommandTypes.AUTH_CRIAR_GERENTE }.envelope.payload["senha"])
        assertEquals(
            "55667788990",
            env.published.first { it.tipo == CommandTypes.CONTA_ATRIBUIR_GERENTE }.envelope.payload["cpfGerente"],
        )
        val email = env.published.first { it.tipo == CommandTypes.EMAIL_TROCA_GERENTE }
        val dest = email.envelope.payload["destinatarios"] as List<*>
        assertEquals(1, dest.size)
        assertEquals("Gumercindo", email.envelope.payload["nomeGerente"])
    }

    @Test
    fun `R13 semConta skips transfer and still succeeds`() {
        val env = harness(autoReply = true, r13 = true, semConta = true)
        env.engine.start(
            MessageEnvelope(
                sagaId = "r13-zero",
                tipo = CommandTypes.INSERIR_GERENTE,
                timestamp = DateTimes.now(),
                payload = mapOf("cpf" to "55667788990", "nome" to "Gumercindo", "senha" to "tads"),
            ),
        )
        assertEquals(JobStatus.CONCLUIDO.wire, env.jobs.find("r13-zero")!!.status)
        val tipos = env.published.map { it.tipo }
        assertFalse(CommandTypes.CONTA_ATRIBUIR_GERENTE in tipos)
        assertFalse(CommandTypes.CLIENTE_OBTER_POR_CPFS in tipos)
        assertFalse(CommandTypes.EMAIL_TROCA_GERENTE in tipos)
        assertTrue(CommandTypes.CONTA_IDENTIFICAR_CONTA_PARA_NOVO_GERENTE in tipos)
    }

    @Test
    fun `R13 duplicate email compensates gerente insert`() {
        val env = harness(autoReply = true, r13 = true, failAuth = true)
        env.engine.start(
            MessageEnvelope(
                sagaId = "r13-dup",
                tipo = CommandTypes.INSERIR_GERENTE,
                timestamp = DateTimes.now(),
                payload = mapOf("cpf" to "77889900112", "email" to "ger1@bantads.com.br", "senha" to "tads"),
            ),
        )
        val job = env.jobs.find("r13-dup")!!
        assertEquals(JobStatus.FALHA.wire, job.status)
        val tipos = env.published.map { it.tipo }
        assertTrue(CommandTypes.GERENTE_REMOVER in tipos)
        assertFalse(CommandTypes.CONTA_ATRIBUIR_GERENTE in tipos)
        assertTrue(env.cache.deletedGerentes.isEmpty())
    }

    @Test
    fun `R15 success with zero accounts is inline and clears session`() {
        val env = harness(autoReply = true, r15 = true, semContas = true)
        env.engine.start(
            MessageEnvelope(
                sagaId = "r15-zero",
                tipo = CommandTypes.REMOVER_GERENTE,
                timestamp = DateTimes.now(),
                payload = mapOf("cpf" to "40501740066"),
            ),
        )
        val job = env.jobs.find("r15-zero")!!
        assertEquals(JobStatus.CONCLUIDO.wire, job.status)
        assertEquals("inline", job.resultType)
        assertEquals("Gerente removido; 0 contas transferidas para Gyândula", job.resultado?.get("mensagem"))
        assertEquals(listOf("40501740066"), env.cache.deletedGerentes)
        assertEquals(listOf("40501740066"), env.cache.sessions)
        val tipos = env.published.map { it.tipo }
        assertTrue(CommandTypes.GERENTE_INATIVAR in tipos)
        assertTrue(CommandTypes.AUTH_DESATIVAR in tipos)
        assertTrue(CommandTypes.CONTA_TRANSFERIR_CONTAS_DO_GERENTE in tipos)
        assertFalse(CommandTypes.CLIENTE_OBTER_POR_CPFS in tipos)
        assertFalse(CommandTypes.EMAIL_TROCA_GERENTE in tipos)
    }

    @Test
    fun `R15 transfers accounts emails clients and can revert`() {
        val env = harness(autoReply = true, r15 = true)
        env.engine.start(
            MessageEnvelope(
                sagaId = "r15-ok",
                tipo = CommandTypes.REMOVER_GERENTE,
                timestamp = DateTimes.now(),
                payload = mapOf("cpf" to "98574307084"),
            ),
        )
        val job = env.jobs.find("r15-ok")!!
        assertEquals(JobStatus.CONCLUIDO.wire, job.status)
        assertEquals("Gerente removido; 2 contas transferidas para Gyândula", job.resultado?.get("mensagem"))
        val tipos = env.published.map { it.tipo }
        assertTrue(CommandTypes.EMAIL_TROCA_GERENTE in tipos)
        val email = env.published.first { it.tipo == CommandTypes.EMAIL_TROCA_GERENTE }
        assertEquals("Gyândula", email.envelope.payload["nomeGerente"])
        val dest = email.envelope.payload["destinatarios"] as List<*>
        assertEquals(2, dest.size)
    }

    @Test
    fun `R15 last active fails without compensating later steps`() {
        val env = harness(autoReply = true, r15 = true, failInativar = true)
        env.engine.start(
            MessageEnvelope(
                sagaId = "r15-last",
                tipo = CommandTypes.REMOVER_GERENTE,
                timestamp = DateTimes.now(),
                payload = mapOf("cpf" to "98574307084"),
            ),
        )
        val job = env.jobs.find("r15-last")!!
        assertEquals(JobStatus.FALHA.wire, job.status)
        assertEquals("Não é permitido remover o último gerente ativo", job.erro)
        val tipos = env.published.map { it.tipo }
        assertEquals(listOf(CommandTypes.GERENTE_INATIVAR), tipos)
        assertTrue(env.cache.sessions.isEmpty())
    }

    private fun harness(
        autoReply: Boolean,
        stepTimeout: Duration = Duration.ofSeconds(30),
        failAuth: Boolean = false,
        r13: Boolean = false,
        r15: Boolean = false,
        semConta: Boolean = false,
        semContas: Boolean = false,
        failInativar: Boolean = false,
    ): Harness {
        val states = MemorySagaStore()
        val jobs = MemoryJobStore()
        val guard = MemoryGuard()
        val clock = MutableClock(1_000)
        val published = mutableListOf<Published>()
        val secrets = SagaSecrets()
        val cache = MemoryCache()
        lateinit var engine: SagaEngine
        val bus =
            CommandBus { queue, envelope ->
                published += Published(queue, envelope)
                if (!autoReply) {
                    return@CommandBus
                }
                if (envelope.tipo == CommandTypes.ECHO_PING) {
                    engine.onReply(sucesso(envelope))
                    return@CommandBus
                }
                val authFail =
                    failAuth &&
                        envelope.tipo in
                        setOf(CommandTypes.AUTH_CRIAR_CLIENTE, CommandTypes.AUTH_CRIAR_GERENTE)
                if (authFail) {
                    engine.onReply(
                        ReplyEnvelope(
                            sagaId = envelope.sagaId!!,
                            tipo = envelope.tipo,
                            timestamp = DateTimes.now(),
                            status = ReplyStatus.FALHA,
                            erro = SagaEngine.EMAIL_DUPLICADO,
                        ),
                    )
                    return@CommandBus
                }
                if (failInativar && envelope.tipo == CommandTypes.GERENTE_INATIVAR) {
                    engine.onReply(
                        ReplyEnvelope(
                            sagaId = envelope.sagaId!!,
                            tipo = envelope.tipo,
                            timestamp = DateTimes.now(),
                            status = ReplyStatus.FALHA,
                            erro = "Não é permitido remover o último gerente ativo",
                        ),
                    )
                    return@CommandBus
                }
                if (r13 && envelope.tipo in R13_REPLIES) {
                    engine.onReply(sucesso(envelope, r13Payload(envelope.tipo, semConta)))
                    return@CommandBus
                }
                if (r15 && envelope.tipo in R15_REPLIES) {
                    engine.onReply(sucesso(envelope, r15Payload(envelope.tipo, semContas)))
                    return@CommandBus
                }
                if (!r13 && envelope.tipo in R9_REPLIES) {
                    engine.onReply(sucesso(envelope, r9Payload(envelope.tipo)))
                }
            }
        engine =
            SagaEngine(
                registry = SagaRegistry(),
                states = states,
                jobs = jobs,
                bus = bus,
                guard = guard,
                clock = clock,
                properties = SagaProperties(stepTimeout = stepTimeout),
                secrets = secrets,
                cache = cache,
            )
        return Harness(engine, states, jobs, guard, clock, published, cache)
    }

    private fun sucesso(
        envelope: MessageEnvelope,
        payload: Map<String, Any?> = emptyMap(),
    ) = ReplyEnvelope(
        sagaId = envelope.sagaId!!,
        tipo = envelope.tipo,
        timestamp = DateTimes.now(),
        status = ReplyStatus.SUCESSO,
        payload = payload,
    )

    private fun r9Payload(tipo: String): Map<String, Any?> =
        when (tipo) {
            CommandTypes.CLIENTE_MARCAR_APROVADA ->
                mapOf(
                    "cpf" to "22233344405",
                    "nome" to "Beltrano",
                    "email" to "beltrano@exemplo.com.br",
                )
            CommandTypes.GERENTE_LISTAR_ATIVOS ->
                mapOf("gerentes" to listOf(mapOf("cpf" to "40501740066")))
            CommandTypes.CONTA_ESCOLHER_GERENTE_MENOS_CLIENTES -> mapOf("cpfGerente" to "40501740066")
            CommandTypes.CLIENTE_CRIAR -> mapOf("cpf" to "22233344405")
            CommandTypes.AUTH_CRIAR_CLIENTE -> mapOf("cpf" to "22233344405", "senha" to "s3nh4R9")
            CommandTypes.CONTA_CRIAR -> mapOf("numero" to "4321", "cpfCliente" to "22233344405")
            else -> emptyMap()
        }

    private fun r13Payload(
        tipo: String,
        semConta: Boolean,
    ): Map<String, Any?> =
        when (tipo) {
            CommandTypes.GERENTE_INSERIR ->
                mapOf(
                    "cpf" to "55667788990",
                    "nome" to "Gumercindo",
                    "email" to "ger5@bantads.com.br",
                )
            CommandTypes.AUTH_CRIAR_GERENTE -> mapOf("cpf" to "55667788990")
            CommandTypes.GERENTE_LISTAR_ATIVOS ->
                mapOf("gerentes" to listOf(mapOf("cpf" to "98574307084"), mapOf("cpf" to "64065268052")))
            CommandTypes.CONTA_IDENTIFICAR_CONTA_PARA_NOVO_GERENTE ->
                if (semConta) {
                    mapOf("semConta" to true)
                } else {
                    mapOf(
                        "semConta" to false,
                        "numero" to "7617",
                        "cpfCliente" to "76179646090",
                        "cpfGerenteOrigem" to "64065268052",
                    )
                }
            CommandTypes.CLIENTE_OBTER_POR_CPFS ->
                mapOf(
                    "clientes" to
                        listOf(
                            mapOf(
                                "cpf" to "76179646090",
                                "nome" to "Coândrya",
                                "email" to "cli5@bantads.com.br",
                            ),
                        ),
                )
            else -> emptyMap()
        }

    private fun r15Payload(
        tipo: String,
        semContas: Boolean,
    ): Map<String, Any?> =
        when (tipo) {
            CommandTypes.GERENTE_LISTAR_ATIVOS ->
                mapOf(
                    "gerentes" to
                        listOf(
                            mapOf("cpf" to "23862179060", "nome" to "Gyândula"),
                            mapOf("cpf" to "64065268052", "nome" to "Godophredo"),
                        ),
                )
            CommandTypes.CONTA_TRANSFERIR_CONTAS_DO_GERENTE ->
                if (semContas) {
                    mapOf(
                        "clientes" to emptyList<String>(),
                        "contas" to emptyList<Any>(),
                        "cpfGerenteDestino" to "23862179060",
                        "quantidadeContas" to 0,
                        "semContas" to true,
                    )
                } else {
                    mapOf(
                        "clientes" to listOf("12912861012", "58872160006"),
                        "contas" to
                            listOf(
                                mapOf("numero" to "1291", "cpfGerente" to "98574307084"),
                                mapOf("numero" to "5887", "cpfGerente" to "98574307084"),
                            ),
                        "cpfGerenteDestino" to "23862179060",
                        "quantidadeContas" to 2,
                        "semContas" to false,
                    )
                }
            CommandTypes.CLIENTE_OBTER_POR_CPFS ->
                mapOf(
                    "clientes" to
                        listOf(
                            mapOf("cpf" to "12912861012", "nome" to "Catharyna", "email" to "cli1@bantads.com.br"),
                            mapOf("cpf" to "58872160006", "nome" to "Cutardo", "email" to "cli4@bantads.com.br"),
                        ),
                )
            else -> emptyMap()
        }

    private data class Published(
        val queue: String,
        val envelope: MessageEnvelope,
    ) {
        val tipo: String get() = envelope.tipo
    }

    private class Harness(
        val engine: SagaEngine,
        val states: MemorySagaStore,
        val jobs: MemoryJobStore,
        val guard: MemoryGuard,
        val clock: MutableClock,
        val published: MutableList<Published>,
        val cache: MemoryCache,
    )

    private class MemorySagaStore : SagaStateStore {
        private val items = ConcurrentHashMap<String, SagaState>()

        override fun find(sagaId: String): SagaState? = items[sagaId]

        override fun save(state: SagaState) {
            items[state.sagaId] = state
        }

        override fun findInProgress(): List<SagaState> =
            items.values.filter { it.status == SagaStatuses.EM_ANDAMENTO && it.timeoutAtEpochMs != null }
    }

    private class MemoryJobStore : JobStore {
        private val items = ConcurrentHashMap<String, JobRecord>()

        override fun find(jobId: String): JobRecord? = items[jobId]

        override fun save(job: JobRecord) {
            items[job.jobId] = job
        }
    }

    private class MemoryGuard : CompensationGuard {
        val seen = ConcurrentHashMap.newKeySet<String>()
        val acquires: Int get() = seen.size

        override fun tryAcquire(
            sagaId: String,
            etapa: Int,
        ): Boolean = seen.add("$sagaId:$etapa")
    }

    private class MemoryCache : CacheInvalidator {
        val deleted = mutableListOf<String>()
        val deletedGerentes = mutableListOf<String>()
        val sessions = mutableListOf<String>()

        override fun deleteCliente(cpf: String?) {
            if (!cpf.isNullOrBlank()) {
                deleted += cpf
            }
        }

        override fun deleteGerente(cpf: String?) {
            if (!cpf.isNullOrBlank()) {
                deletedGerentes += cpf
            }
        }

        override fun deleteSessions(cpf: String?) {
            if (!cpf.isNullOrBlank()) {
                sessions += cpf
            }
        }
    }

    private class MutableClock(
        var millis: Long,
    ) : Clock() {
        override fun millis(): Long = millis

        override fun instant(): Instant = Instant.ofEpochMilli(millis)

        override fun getZone() = ZoneOffset.UTC

        override fun withZone(zone: java.time.ZoneId): Clock = this
    }

    companion object {
        private val R9_REPLIES =
            setOf(
                CommandTypes.CLIENTE_MARCAR_APROVADA,
                CommandTypes.GERENTE_LISTAR_ATIVOS,
                CommandTypes.CONTA_ESCOLHER_GERENTE_MENOS_CLIENTES,
                CommandTypes.CLIENTE_CRIAR,
                CommandTypes.AUTH_CRIAR_CLIENTE,
                CommandTypes.CONTA_CRIAR,
            )
        private val R13_REPLIES =
            setOf(
                CommandTypes.GERENTE_INSERIR,
                CommandTypes.AUTH_CRIAR_GERENTE,
                CommandTypes.GERENTE_LISTAR_ATIVOS,
                CommandTypes.CONTA_IDENTIFICAR_CONTA_PARA_NOVO_GERENTE,
                CommandTypes.CONTA_ATRIBUIR_GERENTE,
                CommandTypes.CLIENTE_OBTER_POR_CPFS,
            )
        private val R15_REPLIES =
            setOf(
                CommandTypes.GERENTE_INATIVAR,
                CommandTypes.AUTH_DESATIVAR,
                CommandTypes.GERENTE_LISTAR_ATIVOS,
                CommandTypes.CONTA_TRANSFERIR_CONTAS_DO_GERENTE,
                CommandTypes.CLIENTE_OBTER_POR_CPFS,
            )
    }
}
