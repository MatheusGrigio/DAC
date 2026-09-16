package br.ufpr.dac.bantads.auth.saga

import br.ufpr.dac.bantads.auth.user.AuthService
import br.ufpr.dac.bantads.auth.user.ResultadoCriacao
import br.ufpr.dac.bantads.shared.amqp.CommandTypes
import br.ufpr.dac.bantads.shared.amqp.MessageEnvelope
import br.ufpr.dac.bantads.shared.amqp.ReplyEnvelope
import br.ufpr.dac.bantads.shared.amqp.ReplyStatus
import br.ufpr.dac.bantads.shared.json.BantadsJson
import br.ufpr.dac.bantads.shared.time.DateTimes
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.stereotype.Service

@Service
class AuthCommandHandler(
    private val authService: AuthService,
    private val inbox: SagaInboxRepository,
) {
    private val mapper = BantadsJson.mapper()

    fun handle(envelope: MessageEnvelope): ReplyEnvelope {
        val sagaId = envelope.sagaId ?: return falha(envelope, "sagaId ausente")
        inbox.findBySagaIdAndTipo(sagaId, envelope.tipo)?.let { saved ->
            return mapper.readValue(saved.replyJson)
        }
        val reply = executar(envelope, sagaId)
        inbox.save(SagaInbox(sagaId = sagaId, tipo = envelope.tipo, replyJson = mapper.writeValueAsString(reply)))
        return reply
    }

    private fun executar(
        envelope: MessageEnvelope,
        sagaId: String,
    ): ReplyEnvelope {
        val payload = envelope.payload
        return when (envelope.tipo) {
            CommandTypes.AUTH_CRIAR_CLIENTE -> criarCliente(envelope, sagaId, payload)
            CommandTypes.AUTH_CRIAR_GERENTE -> criarGerente(envelope, sagaId, payload)
            CommandTypes.AUTH_REMOVER -> {
                val cpf = texto(payload, "cpf") ?: return falha(envelope, "cpf ausente", sagaId)
                authService.remover(cpf)
                sucesso(envelope, sagaId)
            }
            CommandTypes.AUTH_DESATIVAR -> {
                val cpf = texto(payload, "cpf") ?: return falha(envelope, "cpf ausente", sagaId)
                if (!authService.desativar(cpf)) return falha(envelope, "Usuário não encontrado", sagaId)
                sucesso(envelope, sagaId)
            }
            CommandTypes.AUTH_REATIVAR -> {
                val cpf = texto(payload, "cpf") ?: return falha(envelope, "cpf ausente", sagaId)
                if (!authService.reativar(cpf)) return falha(envelope, "Usuário não encontrado", sagaId)
                sucesso(envelope, sagaId)
            }
            else -> falha(envelope, "tipo desconhecido", sagaId)
        }
    }

    private fun criarCliente(
        envelope: MessageEnvelope,
        sagaId: String,
        payload: Map<String, Any?>,
    ): ReplyEnvelope {
        val cpf = texto(payload, "cpf") ?: return falha(envelope, "cpf ausente", sagaId)
        val email = texto(payload, "email", "login") ?: return falha(envelope, "email ausente", sagaId)
        return when (val resultado = authService.criarCliente(cpf, email)) {
            is ResultadoCriacao.Sucesso ->
                sucesso(envelope, sagaId, mapOf("cpf" to resultado.cpf, "senha" to resultado.senhaClara))
            is ResultadoCriacao.Falha -> falha(envelope, resultado.erro, sagaId)
        }
    }

    private fun criarGerente(
        envelope: MessageEnvelope,
        sagaId: String,
        payload: Map<String, Any?>,
    ): ReplyEnvelope {
        val cpf = texto(payload, "cpf") ?: return falha(envelope, "cpf ausente", sagaId)
        val email = texto(payload, "email", "login") ?: return falha(envelope, "email ausente", sagaId)
        val senha = texto(payload, "senha") ?: return falha(envelope, "senha ausente", sagaId)
        return when (val resultado = authService.criarGerente(cpf, email, senha)) {
            is ResultadoCriacao.Sucesso -> sucesso(envelope, sagaId, mapOf("cpf" to resultado.cpf))
            is ResultadoCriacao.Falha -> falha(envelope, resultado.erro, sagaId)
        }
    }

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
