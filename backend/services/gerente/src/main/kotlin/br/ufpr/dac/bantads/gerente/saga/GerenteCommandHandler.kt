package br.ufpr.dac.bantads.gerente.saga

import br.ufpr.dac.bantads.gerente.cadastro.GerenteEntity
import br.ufpr.dac.bantads.gerente.cadastro.GerenteService
import br.ufpr.dac.bantads.shared.amqp.CommandTypes
import br.ufpr.dac.bantads.shared.amqp.MessageEnvelope
import br.ufpr.dac.bantads.shared.amqp.ReplyEnvelope
import br.ufpr.dac.bantads.shared.amqp.ReplyStatus
import br.ufpr.dac.bantads.shared.json.BantadsJson
import br.ufpr.dac.bantads.shared.time.DateTimes
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.stereotype.Service

@Service
class GerenteCommandHandler(
    private val gerentes: GerenteService,
    private val inbox: SagaInboxRepository,
) {
    private val mapper = BantadsJson.mapper()

    fun handle(envelope: MessageEnvelope): ReplyEnvelope {
        val sagaId = envelope.sagaId ?: return falha(envelope, "sagaId ausente")
        inbox.findBySagaIdAndTipo(sagaId, envelope.tipo)?.let { saved ->
            return mapper.readValue(saved.replyJson)
        }
        val reply = executar(envelope, sagaId)
        inbox.save(SagaInboxEntity(sagaId = sagaId, tipo = envelope.tipo, replyJson = mapper.writeValueAsString(reply)))
        return reply
    }

    private fun executar(
        envelope: MessageEnvelope,
        sagaId: String,
    ): ReplyEnvelope {
        val payload = envelope.payload
        val cpf = texto(payload, "cpf")
        return try {
            when (envelope.tipo) {
                CommandTypes.GERENTE_INSERIR -> {
                    val alvo = cpf ?: return falha(envelope, "cpf ausente", sagaId)
                    val nome = texto(payload, "nome") ?: return falha(envelope, "nome ausente", sagaId)
                    val email = texto(payload, "email", "login") ?: return falha(envelope, "email ausente", sagaId)
                    val telefone = texto(payload, "telefone") ?: return falha(envelope, "telefone ausente", sagaId)
                    val saved = gerentes.inserir(alvo, nome, email, telefone)
                    sucesso(envelope, sagaId, gerentePayload(saved))
                }
                CommandTypes.GERENTE_REMOVER -> {
                    val alvo = cpf ?: return falha(envelope, "cpf ausente", sagaId)
                    gerentes.remover(alvo)
                    sucesso(envelope, sagaId)
                }
                CommandTypes.GERENTE_INATIVAR -> {
                    val alvo = cpf ?: return falha(envelope, "cpf ausente", sagaId)
                    gerentes.inativar(alvo)
                    sucesso(envelope, sagaId)
                }
                CommandTypes.GERENTE_REATIVAR -> {
                    val alvo = cpf ?: return falha(envelope, "cpf ausente", sagaId)
                    gerentes.reativar(alvo)
                    sucesso(envelope, sagaId)
                }
                CommandTypes.GERENTE_LISTAR_ATIVOS -> {
                    val lista = gerentes.listarAtivos().map { gerentePayload(it) }
                    sucesso(envelope, sagaId, mapOf("gerentes" to lista))
                }
                else -> falha(envelope, "tipo desconhecido", sagaId)
            }
        } catch (ex: IllegalStateException) {
            falha(envelope, ex.message ?: "Falha", sagaId)
        }
    }

    private fun gerentePayload(entity: GerenteEntity) =
        mapOf(
            "cpf" to entity.cpf,
            "nome" to entity.nome,
            "email" to entity.email,
            "telefone" to entity.telefone,
            "ativo" to entity.ativo,
        )

    private fun sucesso(
        envelope: MessageEnvelope,
        sagaId: String,
        payload: Map<String, Any?> = emptyMap(),
    ) = ReplyEnvelope(
        sagaId = sagaId,
        tipo = envelope.tipo,
        timestamp = DateTimes.now(),
        status = ReplyStatus.SUCESSO,
        erro = null,
        payload = payload,
    )

    private fun falha(
        envelope: MessageEnvelope,
        erro: String,
        sagaId: String = envelope.sagaId.orEmpty(),
    ) = ReplyEnvelope(
        sagaId = sagaId,
        tipo = envelope.tipo,
        timestamp = DateTimes.now(),
        status = ReplyStatus.FALHA,
        erro = erro,
        payload = emptyMap(),
    )

    private fun texto(
        payload: Map<String, Any?>,
        vararg keys: String,
    ): String? {
        keys.forEach { key ->
            val value = payload[key]?.toString()?.trim().orEmpty()
            if (value.isNotEmpty()) return value
        }
        return null
    }
}
