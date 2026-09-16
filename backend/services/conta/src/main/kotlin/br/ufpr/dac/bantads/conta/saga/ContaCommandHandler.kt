package br.ufpr.dac.bantads.conta.saga

import br.ufpr.dac.bantads.conta.command.http.ContaCommandService
import br.ufpr.dac.bantads.shared.amqp.CommandTypes
import br.ufpr.dac.bantads.shared.amqp.MessageEnvelope
import br.ufpr.dac.bantads.shared.amqp.ReplyEnvelope
import br.ufpr.dac.bantads.shared.amqp.ReplyStatus
import br.ufpr.dac.bantads.shared.json.BantadsJson
import br.ufpr.dac.bantads.shared.time.DateTimes
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.stereotype.Service

@Service
class ContaCommandHandler(
    private val commands: ContaCommandService,
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
        return try {
            when (envelope.tipo) {
                CommandTypes.CONTA_ESCOLHER_GERENTE_MENOS_CLIENTES -> {
                    val cpf =
                        commands.escolherGerenteMenosClientes(cpfs(payload))
                            ?: return falha(envelope, "Nenhum gerente ativo", sagaId)
                    sucesso(envelope, sagaId, mapOf("cpfGerente" to cpf))
                }
                CommandTypes.CONTA_CRIAR -> {
                    val cliente = texto(payload, "cpfCliente", "cpf") ?: return falha(envelope, "cpfCliente ausente", sagaId)
                    val gerente = texto(payload, "cpfGerente") ?: return falha(envelope, "cpfGerente ausente", sagaId)
                    val numero = commands.criar(cliente, gerente)
                    sucesso(envelope, sagaId, mapOf("numero" to numero, "cpfCliente" to cliente))
                }
                CommandTypes.CONTA_REMOVER -> {
                    val numero = texto(payload, "numero") ?: return falha(envelope, "numero ausente", sagaId)
                    commands.removerSeSoCriado(numero)
                    sucesso(envelope, sagaId)
                }
                CommandTypes.CONTA_IDENTIFICAR_CONTA_PARA_NOVO_GERENTE -> {
                    val escolhida = commands.identificarR13(cpfs(payload).toSet())
                    if (escolhida == null) {
                        sucesso(envelope, sagaId, mapOf("semConta" to true))
                    } else {
                        sucesso(
                            envelope,
                            sagaId,
                            mapOf(
                                "semConta" to false,
                                "numero" to escolhida.numero,
                                "cpfCliente" to escolhida.cpfCliente,
                                "cpfGerenteOrigem" to escolhida.cpfGerente,
                            ),
                        )
                    }
                }
                CommandTypes.CONTA_ATRIBUIR_GERENTE, CommandTypes.CONTA_REATRIBUIR_GERENTE -> {
                    val numero = texto(payload, "numero") ?: return falha(envelope, "numero ausente", sagaId)
                    val gerente = texto(payload, "cpfGerente") ?: return falha(envelope, "cpfGerente ausente", sagaId)
                    commands.atribuirGerente(numero, gerente)
                    sucesso(envelope, sagaId)
                }
                CommandTypes.CONTA_TRANSFERIR_CONTAS_DO_GERENTE -> {
                    val removido = texto(payload, "cpf", "cpfGerente") ?: return falha(envelope, "cpf ausente", sagaId)
                    sucesso(envelope, sagaId, commands.transferirContasDoGerente(removido, cpfs(payload)))
                }
                CommandTypes.CONTA_REVERTER_TRANSFERENCIA_GERENTES -> {
                    contasReverter(payload).forEach { (numero, gerente) ->
                        commands.atribuirGerente(numero, gerente)
                    }
                    sucesso(envelope, sagaId)
                }
                else -> falha(envelope, "tipo desconhecido", sagaId)
            }
        } catch (ex: IllegalStateException) {
            falha(envelope, ex.message ?: "Falha", sagaId)
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
            if (value.isNotEmpty() && value != "null") return value
        }
        return null
    }

    private fun cpfs(payload: Map<String, Any?>): List<String> {
        val raw = payload["gerentes"] ?: payload["cpfs"] ?: payload["gerentesAtivos"]
        return when (raw) {
            is Collection<*> ->
                raw.mapNotNull { item ->
                    when (item) {
                        is Map<*, *> -> item["cpf"]?.toString()?.trim()?.takeIf { it.isNotEmpty() }
                        else -> item?.toString()?.trim()?.takeIf { it.isNotEmpty() }
                    }
                }
            is String -> raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }
            else -> emptyList()
        }
    }

    private fun contasReverter(payload: Map<String, Any?>): List<Pair<String, String>> {
        val raw = payload["contas"]
        if (raw !is Collection<*>) return emptyList()
        return raw.mapNotNull { item ->
            val map = item as? Map<*, *> ?: return@mapNotNull null
            val numero = map["numero"]?.toString()?.trim().orEmpty()
            val gerente = map["cpfGerente"]?.toString()?.trim().orEmpty()
            if (numero.isEmpty() || gerente.isEmpty()) null else numero to gerente
        }
    }
}
