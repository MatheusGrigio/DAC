package br.ufpr.dac.bantads.shared.amqp

import br.ufpr.dac.bantads.shared.domain.EventType
import br.ufpr.dac.bantads.shared.domain.SagaType
import br.ufpr.dac.bantads.shared.json.BantadsJson
import com.fasterxml.jackson.module.kotlin.readValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EnvelopeJsonTest {
    private val mapper = BantadsJson.mapper()

    @Test
    fun `command envelope omits sagaId outside of SAGA`() {
        val envelope =
            MessageEnvelope(
                tipo = CommandTypes.CLIENTE_MARCAR_APROVADA,
                timestamp = "2026-04-30T10:00:00",
                payload = emptyMap(),
            )
        val json = mapper.writeValueAsString(envelope)
        assertFalse(json.contains("sagaId"))
        assertEquals("cliente.marcar-aprovada", mapper.readTree(json)["tipo"].asText())
        assertEquals("2026-04-30T10:00:00", mapper.readTree(json)["timestamp"].asText())
    }

    @Test
    fun `command envelope keeps sagaId in SAGA`() {
        val envelope =
            MessageEnvelope(
                sagaId = "8f14e45f-ceea-4e1b-9d2a-52f7254b1f0b",
                tipo = CommandTypes.APROVAR_CLIENTE,
                timestamp = "2026-04-30T10:00:00",
                payload = mapOf("cpf" to "12912861012"),
            )
        val json = mapper.writeValueAsString(envelope)
        val parsed: MessageEnvelope = mapper.readValue(json)
        assertEquals(envelope.sagaId, parsed.sagaId)
        assertEquals(envelope.tipo, parsed.tipo)
        assertEquals("12912861012", parsed.payload["cpf"])
    }

    @Test
    fun `reply envelope keeps null erro`() {
        val envelope =
            ReplyEnvelope(
                sagaId = "8f14e45f-ceea-4e1b-9d2a-52f7254b1f0b",
                tipo = CommandTypes.CLIENTE_MARCAR_APROVADA,
                timestamp = "2026-04-30T10:00:00",
                status = ReplyStatus.SUCESSO,
                erro = null,
            )
        val json = mapper.writeValueAsString(envelope)
        val tree = mapper.readTree(json)
        assertEquals("SUCESSO", tree["status"].asText())
        assertTrue(tree["erro"].isNull)
        val parsed: ReplyEnvelope = mapper.readValue(json)
        assertNull(parsed.erro)
        assertEquals(ReplyStatus.SUCESSO, parsed.status)
    }

    @Test
    fun `reply envelope serializes FALHA with erro`() {
        val envelope =
            ReplyEnvelope(
                sagaId = "8f14e45f-ceea-4e1b-9d2a-52f7254b1f0b",
                tipo = CommandTypes.AUTH_CRIAR_CLIENTE,
                timestamp = "2026-04-30T10:00:00",
                status = ReplyStatus.FALHA,
                erro = "E-mail já cadastrado",
            )
        val json = mapper.writeValueAsString(envelope)
        assertEquals("E-mail já cadastrado", mapper.readTree(json)["erro"].asText())
    }

    @Test
    fun `unknown JSON properties do not fail`() {
        val json =
            """
            {"tipo":"cliente.criar","timestamp":"2026-04-30T10:00:00","payload":{},"extra":true}
            """.trimIndent()
        val parsed: MessageEnvelope = mapper.readValue(json)
        assertEquals(CommandTypes.CLIENTE_CRIAR, parsed.tipo)
    }

    @Test
    fun `event types keep exact accents`() {
        assertEquals("\"Depósito\"", mapper.writeValueAsString(EventType.DEPOSITO))
        assertEquals("\"TransferênciaOrigem\"", mapper.writeValueAsString(EventType.TRANSFERENCIA_ORIGEM))
        assertEquals("\"TransferênciaDestino\"", mapper.writeValueAsString(EventType.TRANSFERENCIA_DESTINO))
        assertEquals(EventType.DEPOSITO, mapper.readValue("\"Depósito\"", EventType::class.java))
    }

    @Test
    fun `saga types use catalog strings`() {
        assertEquals("aprovar-cliente", SagaType.APROVAR_CLIENTE.tipo)
        assertEquals("inserir-gerente", SagaType.INSERIR_GERENTE.tipo)
        assertEquals("remover-gerente", SagaType.REMOVER_GERENTE.tipo)
        assertEquals("echo", SagaType.ECHO.tipo)
        assertEquals("\"aprovar-cliente\"", mapper.writeValueAsString(SagaType.APROVAR_CLIENTE))
    }
}
