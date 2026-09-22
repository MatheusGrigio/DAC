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
import br.ufpr.dac.bantads.shared.domain.ResultType
import br.ufpr.dac.bantads.shared.time.DateTimes
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Clock

@Service
class SagaEngine(
    private val registry: SagaRegistry,
    private val states: SagaStateStore,
    private val jobs: JobStore,
    private val bus: CommandBus,
    private val guard: CompensationGuard,
    private val clock: Clock,
    private val properties: SagaProperties,
    private val secrets: SagaSecrets,
    private val cache: CacheInvalidator,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun start(envelope: MessageEnvelope) {
        val sagaId = envelope.sagaId
        if (sagaId.isNullOrBlank()) {
            log.warn("saga.cmd sem sagaId tipo={}", envelope.tipo)
            return
        }
        if (states.find(sagaId) != null) {
            return
        }
        val definition = registry.get(envelope.tipo)
        if (definition == null) {
            log.warn("saga tipo desconhecido={}", envelope.tipo)
            failJob(sagaId, "SAGA desconhecida: ${envelope.tipo}")
            return
        }
        val now = DateTimes.now()
        val senha = envelope.payload["senha"]?.toString()?.trim().orEmpty()
        if (senha.isNotEmpty()) {
            secrets.put(sagaId, senha)
        }
        val payload = envelope.payload.withoutPassword()
        if (jobs.find(sagaId) == null) {
            jobs.save(
                JobRecord(
                    jobId = sagaId,
                    status = JobStatus.PENDENTE.wire,
                ),
            )
        }
        val state =
            SagaState(
                sagaId = sagaId,
                tipo = envelope.tipo,
                etapaAtual = 0,
                status = SagaStatuses.EM_ANDAMENTO,
                payload = payload,
                timestamp = now,
            )
        states.save(state)
        advance(state, definition)
    }

    fun onReply(reply: ReplyEnvelope) {
        val state = states.find(reply.sagaId) ?: return
        if (state.status != SagaStatuses.EM_ANDAMENTO) {
            return
        }
        if (state.waitingTipo != null && reply.tipo != state.waitingTipo) {
            return
        }
        val definition = registry.get(state.tipo) ?: return
        if (reply.status == ReplyStatus.FALHA) {
            compensate(state, definition, reply.erro ?: "FALHA", reply.tipo)
            return
        }
        capturePassword(state.sagaId, reply)
        val succeeded = state.succeededTipos + reply.tipo
        val merged = state.payload + reply.payload.withoutPassword()
        val next =
            state.copy(
                etapaAtual = state.etapaAtual + 1,
                payload = merged,
                waitingTipo = null,
                timeoutAtEpochMs = null,
                succeededTipos = succeeded,
            )
        states.save(next)
        advance(next, definition)
    }

    fun onDlq(envelope: MessageEnvelope) {
        val sagaId = envelope.sagaId ?: return
        val state = states.find(sagaId) ?: return
        if (state.status != SagaStatuses.EM_ANDAMENTO) {
            return
        }
        val definition = registry.get(state.tipo) ?: return
        compensate(state, definition, "DLQ", envelope.tipo)
    }

    fun failTimedOutSteps() {
        val now = clock.millis()
        states.findInProgress().forEach { state ->
            val deadline = state.timeoutAtEpochMs ?: return@forEach
            if (now >= deadline) {
                val definition = registry.get(state.tipo) ?: return@forEach
                compensate(state, definition, "timeout", state.waitingTipo)
            }
        }
    }

    private fun advance(
        state: SagaState,
        definition: SagaDefinition,
    ) {
        if (state.etapaAtual >= definition.steps.size) {
            complete(state)
            return
        }
        val step = definition.steps[state.etapaAtual]
        if (shouldSkip(step, state.payload)) {
            val skipped =
                state.copy(
                    etapaAtual = state.etapaAtual + 1,
                    waitingTipo = null,
                    timeoutAtEpochMs = null,
                )
            states.save(skipped)
            advance(skipped, definition)
            return
        }
        when (step.kind) {
            StepKind.FIRE_AND_FORGET -> {
                publish(step.queue, state, step.tipo)
                val next =
                    state.copy(
                        etapaAtual = state.etapaAtual + 1,
                        succeededTipos = state.succeededTipos + step.tipo,
                    )
                states.save(next)
                advance(next, definition)
            }
            StepKind.LOCAL -> {
                val executed = executeLocal(state, step.tipo)
                val next =
                    executed.copy(
                        etapaAtual = state.etapaAtual + 1,
                        succeededTipos = executed.succeededTipos + step.tipo,
                        waitingTipo = null,
                        timeoutAtEpochMs = null,
                    )
                states.save(next)
                advance(next, definition)
            }
            StepKind.TRANSACTIONAL -> {
                val etapa = state.etapaAtual
                publish(step.queue, state, step.tipo)
                val current = states.find(state.sagaId) ?: return
                if (current.status != SagaStatuses.EM_ANDAMENTO || current.etapaAtual != etapa) {
                    return
                }
                states.save(
                    current.copy(
                        waitingTipo = step.tipo,
                        timeoutAtEpochMs = clock.millis() + properties.stepTimeout.toMillis(),
                    ),
                )
            }
        }
    }

    private fun compensate(
        state: SagaState,
        definition: SagaDefinition,
        erro: String,
        failingTipo: String?,
    ) {
        if (!guard.tryAcquire(state.sagaId, state.etapaAtual)) {
            return
        }
        val emailDuplicado = isEmailDuplicado(failingTipo, erro)
        var current = state
        definition.steps
            .take(state.etapaAtual)
            .withIndex()
            .reversed()
            .forEach { (index, step) ->
                val succeeded = step.tipo in current.succeededTipos
                if (!succeeded) {
                    return@forEach
                }
                val compensation = compensationFor(step, emailDuplicado) ?: return@forEach
                when (compensation.kind) {
                    StepKind.LOCAL, StepKind.FIRE_AND_FORGET -> {
                        current = executeLocal(current, compensation.tipo)
                        if (compensation.kind == StepKind.FIRE_AND_FORGET) {
                            publish(compensation.queue, current, compensation.tipo)
                        }
                    }
                    StepKind.TRANSACTIONAL -> {
                        publish(compensation.queue, current, compensation.tipo)
                    }
                }
                log.info(
                    "saga compensate sagaId={} etapa={} tipo={}",
                    state.sagaId,
                    index,
                    compensation.tipo,
                )
            }
        if (state.tipo == CommandTypes.APROVAR_CLIENTE) {
            publish(
                QueueNames.MS_EMAIL_CMD,
                current,
                CommandTypes.EMAIL_FALHA_APROVACAO,
                mapOf("motivo" to erro),
            )
        }
        val failed =
            current.copy(
                status = SagaStatuses.FALHA,
                waitingTipo = null,
                timeoutAtEpochMs = null,
            )
        states.save(failed)
        failJob(state.sagaId, erro)
    }

    private fun compensationFor(
        step: SagaStep,
        emailDuplicado: Boolean,
    ): Compensation? {
        if (emailDuplicado && step.tipo == CommandTypes.CLIENTE_MARCAR_APROVADA) {
            return Compensation(
                tipo = CommandTypes.CLIENTE_MARCAR_NAO_APROVADA,
                kind = StepKind.TRANSACTIONAL,
                queue = step.compensationQueue ?: QueueNames.MS_CLIENTE_CMD,
            )
        }
        val tipo = step.compensationTipo ?: return null
        return Compensation(
            tipo = tipo,
            kind = step.compensationKind ?: StepKind.TRANSACTIONAL,
            queue = step.compensationQueue,
        )
    }

    private fun executeLocal(
        state: SagaState,
        tipo: String,
    ): SagaState =
        when (tipo) {
            CommandTypes.ECHO_UNDO -> state.copy(compensacoes = state.compensacoes + 1)
            CommandTypes.SAGA_INVALIDAR_SESSAO -> {
                cache.deleteSessions(state.payload["cpf"]?.toString())
                state
            }
            else -> state
        }

    private fun publish(
        queue: String?,
        state: SagaState,
        tipo: String,
        extra: Map<String, Any?> = emptyMap(),
    ) {
        if (queue.isNullOrBlank()) {
            return
        }
        val extras = extrasFor(tipo, state) + extra
        if (tipo == CommandTypes.EMAIL_TROCA_GERENTE) {
            val dest = extras["destinatarios"] as? Collection<*>
            if (dest.isNullOrEmpty()) {
                return
            }
        }
        bus.publish(
            queue,
            MessageEnvelope(
                sagaId = state.sagaId,
                tipo = tipo,
                timestamp = DateTimes.now(),
                payload = state.payload + extras,
            ),
        )
    }

    private fun extrasFor(
        tipo: String,
        state: SagaState,
    ): Map<String, Any?> =
        when (tipo) {
            CommandTypes.EMAIL_SENHA_CLIENTE ->
                secrets.take(state.sagaId)?.let { mapOf("senha" to it) } ?: emptyMap()
            CommandTypes.AUTH_CRIAR_GERENTE ->
                secrets.peek(state.sagaId)?.let { mapOf("senha" to it) } ?: emptyMap()
            CommandTypes.CONTA_ATRIBUIR_GERENTE ->
                mapOf("cpfGerente" to state.payload["cpf"])
            CommandTypes.CONTA_REATRIBUIR_GERENTE ->
                mapOf("cpfGerente" to state.payload["cpfGerenteOrigem"])
            CommandTypes.CLIENTE_OBTER_POR_CPFS -> mapOf("cpfs" to cpfsParaObter(state.payload))
            CommandTypes.EMAIL_TROCA_GERENTE -> emailTrocaExtras(state.payload)
            else -> emptyMap()
        }

    private fun cpfsParaObter(payload: Map<String, Any?>): List<String> {
        val raw = payload["clientes"] ?: payload["cpfs"]
        val fromList =
            when (raw) {
                is Collection<*> ->
                    raw.mapNotNull { item ->
                        when (item) {
                            is Map<*, *> -> item["cpf"]?.toString()
                            else -> item?.toString()
                        }?.trim()?.takeIf { it.isNotEmpty() }
                    }
                else -> emptyList()
            }
        if (fromList.isNotEmpty()) {
            return fromList
        }
        return listOfNotNull(payload["cpfCliente"]?.toString()?.trim()?.takeIf { it.isNotEmpty() })
    }

    private fun emailTrocaExtras(payload: Map<String, Any?>): Map<String, Any?> {
        val clientes = payload["clientes"] as? Collection<*> ?: emptyList<Any?>()
        val destinatarios =
            clientes.mapNotNull { item ->
                val map = item as? Map<*, *> ?: return@mapNotNull null
                val email = map["email"]?.toString()?.trim().orEmpty()
                if (email.isEmpty()) {
                    null
                } else {
                    mapOf("email" to email, "nome" to map["nome"])
                }
            }
        return mapOf(
            "destinatarios" to destinatarios,
            "nomeGerente" to (
                nomeDoGerente(payload, payload["cpfGerenteDestino"]?.toString())
                    ?: payload["nome"]
            ),
        )
    }

    private fun nomeDoGerente(
        payload: Map<String, Any?>,
        cpf: String?,
    ): String? {
        val alvo = cpf?.trim().orEmpty()
        if (alvo.isEmpty()) {
            return null
        }
        val gerentes = payload["gerentes"] as? Collection<*> ?: return null
        return gerentes
            .mapNotNull { it as? Map<*, *> }
            .firstOrNull { it["cpf"]?.toString() == alvo }
            ?.get("nome")
            ?.toString()
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    }

    private fun quantidadeContas(payload: Map<String, Any?>): Int {
        val raw = payload["quantidadeContas"]
        when (raw) {
            is Number -> return raw.toInt()
            is String -> raw.toIntOrNull()?.let { return it }
        }
        return (payload["contas"] as? Collection<*>)?.size ?: 0
    }

    private fun shouldSkip(
        step: SagaStep,
        payload: Map<String, Any?>,
    ): Boolean {
        val key = step.skipIfTrue ?: return false
        val value = payload[key] ?: return false
        return value == true || value.toString().equals("true", ignoreCase = true)
    }

    private fun complete(state: SagaState) {
        secrets.clear(state.sagaId)
        val done = state.copy(status = SagaStatuses.CONCLUIDO, waitingTipo = null, timeoutAtEpochMs = null)
        states.save(done)
        when (state.tipo) {
            CommandTypes.ECHO -> {
                val resultado = mapOf("mensagem" to "pong")
                val current = jobs.find(state.sagaId)
                jobs.save(
                    (current ?: JobRecord(jobId = state.sagaId, status = JobStatus.CONCLUIDO.wire)).copy(
                        status = JobStatus.CONCLUIDO.wire,
                        resultType = ResultType.INLINE.wire,
                        resultado = resultado,
                    ),
                )
            }
            CommandTypes.APROVAR_CLIENTE -> {
                val cpf = state.payload["cpf"]?.toString()
                val current = jobs.find(state.sagaId)
                jobs.save(
                    (current ?: JobRecord(jobId = state.sagaId, status = JobStatus.CONCLUIDO.wire)).copy(
                        status = JobStatus.CONCLUIDO.wire,
                        resultType = ResultType.RESOURCE.wire,
                        dominio = "clientes",
                        resourceId = cpf,
                    ),
                )
                cache.deleteCliente(cpf)
            }
            CommandTypes.INSERIR_GERENTE -> {
                val cpf = state.payload["cpf"]?.toString()
                val current = jobs.find(state.sagaId)
                jobs.save(
                    (current ?: JobRecord(jobId = state.sagaId, status = JobStatus.CONCLUIDO.wire)).copy(
                        status = JobStatus.CONCLUIDO.wire,
                        resultType = ResultType.RESOURCE.wire,
                        dominio = "gerentes",
                        resourceId = cpf,
                    ),
                )
                cache.deleteGerente(cpf)
            }
            CommandTypes.REMOVER_GERENTE -> {
                val cpf = state.payload["cpf"]?.toString()
                val n = quantidadeContas(state.payload)
                val dest =
                    nomeDoGerente(state.payload, state.payload["cpfGerenteDestino"]?.toString())
                        ?: "gerente"
                val resultado = mapOf("mensagem" to "Gerente removido; $n contas transferidas para $dest")
                val current = jobs.find(state.sagaId)
                jobs.save(
                    (current ?: JobRecord(jobId = state.sagaId, status = JobStatus.CONCLUIDO.wire)).copy(
                        status = JobStatus.CONCLUIDO.wire,
                        resultType = ResultType.INLINE.wire,
                        resultado = resultado,
                    ),
                )
                cache.deleteGerente(cpf)
            }
            else -> {
                val current = jobs.find(state.sagaId)
                jobs.save(
                    (current ?: JobRecord(jobId = state.sagaId, status = JobStatus.CONCLUIDO.wire)).copy(
                        status = JobStatus.CONCLUIDO.wire,
                    ),
                )
            }
        }
    }

    private fun failJob(
        jobId: String,
        erro: String,
    ) {
        secrets.clear(jobId)
        val current = jobs.find(jobId)
        jobs.save(
            (current ?: JobRecord(jobId = jobId, status = JobStatus.FALHA.wire)).copy(
                status = JobStatus.FALHA.wire,
                erro = erro,
            ),
        )
    }

    private fun capturePassword(
        sagaId: String,
        reply: ReplyEnvelope,
    ) {
        if (reply.tipo != CommandTypes.AUTH_CRIAR_CLIENTE) {
            return
        }
        val senha = reply.payload["senha"]?.toString()?.trim().orEmpty()
        if (senha.isNotEmpty()) {
            secrets.put(sagaId, senha)
        }
    }

    private data class Compensation(
        val tipo: String,
        val kind: StepKind,
        val queue: String?,
    )

    companion object {
        const val EMAIL_DUPLICADO = "E-mail já cadastrado"

        fun isEmailDuplicado(
            failingTipo: String?,
            erro: String,
        ): Boolean = failingTipo == CommandTypes.AUTH_CRIAR_CLIENTE && erro == EMAIL_DUPLICADO
    }
}
