package br.ufpr.dac.bantads.cliente.saga

import br.ufpr.dac.bantads.cliente.cadastro.CadastroService
import br.ufpr.dac.bantads.cliente.solicitacao.SolicitacaoEntity
import br.ufpr.dac.bantads.cliente.solicitacao.SolicitacaoService
import br.ufpr.dac.bantads.shared.amqp.CommandTypes
import br.ufpr.dac.bantads.shared.amqp.MessageEnvelope
import br.ufpr.dac.bantads.shared.amqp.ReplyEnvelope
import br.ufpr.dac.bantads.shared.amqp.ReplyStatus
import br.ufpr.dac.bantads.shared.json.BantadsJson
import br.ufpr.dac.bantads.shared.money.Money
import br.ufpr.dac.bantads.shared.time.DateTimes
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.stereotype.Service

@Service
class ClienteCommandHandler(
    private val solicitacoes: SolicitacaoService,
    private val cadastro: CadastroService,
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
        val cpf = texto(envelope.payload, "cpf")
        return try {
            when (envelope.tipo) {
                CommandTypes.CLIENTE_MARCAR_APROVADA -> {
                    val alvo = cpf ?: return falha(envelope, "cpf ausente", sagaId)
                    sucesso(envelope, sagaId, solicitacaoPayload(solicitacoes.marcarAprovada(alvo)))
                }
                CommandTypes.CLIENTE_DESMARCAR_APROVADA -> {
                    val alvo = cpf ?: return falha(envelope, "cpf ausente", sagaId)
                    solicitacoes.desmarcarAprovada(alvo)
                    sucesso(envelope, sagaId)
                }
                CommandTypes.CLIENTE_MARCAR_NAO_APROVADA -> {
                    val alvo = cpf ?: return falha(envelope, "cpf ausente", sagaId)
                    solicitacoes.marcarNaoAprovada(alvo)
                    sucesso(envelope, sagaId)
                }
                CommandTypes.CLIENTE_CRIAR -> {
                    val alvo = cpf ?: return falha(envelope, "cpf ausente", sagaId)
                    cadastro.criarAPartirDaSolicitacao(alvo)
                    sucesso(envelope, sagaId, mapOf("cpf" to alvo))
                }
                CommandTypes.CLIENTE_REMOVER -> {
                    val alvo = cpf ?: return falha(envelope, "cpf ausente", sagaId)
                    cadastro.remover(alvo)
                    sucesso(envelope, sagaId)
                }
                CommandTypes.CLIENTE_OBTER_POR_CPFS -> {
                    val nomes = cadastro.nomesPorCpfs(cpfs(envelope.payload))
                    sucesso(
                        envelope,
                        sagaId,
                        mapOf(
                            "clientes" to
                                nomes.map { mapOf("cpf" to it.cpf, "nome" to it.nome, "email" to it.email) },
                        ),
                    )
                }
                else -> falha(envelope, "tipo desconhecido", sagaId)
            }
        } catch (ex: IllegalStateException) {
            falha(envelope, ex.message ?: "Falha", sagaId)
        }
    }

    private fun solicitacaoPayload(entity: SolicitacaoEntity): Map<String, Any?> =
        mapOf(
            "cpf" to entity.cpf,
            "nome" to entity.nome,
            "email" to entity.email,
            "telefone" to entity.telefone,
            "salario" to Money.format(entity.salario),
            "endereco" to
                mapOf(
                    "logradouro" to entity.endereco.logradouro,
                    "numero" to entity.endereco.numero,
                    "complemento" to entity.endereco.complemento,
                    "cep" to entity.endereco.cep,
                    "cidade" to entity.endereco.cidade,
                    "uf" to entity.endereco.uf,
                ),
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

    private fun cpfs(payload: Map<String, Any?>): List<String> =
        when (val raw = payload["cpfs"]) {
            is Collection<*> -> raw.mapNotNull { it?.toString()?.trim()?.takeIf(String::isNotEmpty) }
            is String -> raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }
            else -> emptyList()
        }
}
