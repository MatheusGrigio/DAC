package br.ufpr.dac.bantads.conta.command.seed

import br.ufpr.dac.bantads.conta.command.event.EventTypes
import br.ufpr.dac.bantads.conta.command.event.StoredEvent
import br.ufpr.dac.bantads.shared.time.DateTimes
import java.nio.charset.StandardCharsets
import java.util.UUID

data class SeedEventSpec(
    val numero: String,
    val tipo: String,
    val timestamp: String,
    val payload: Map<String, Any?>,
)

object SeedContas {
    const val CATHARYNA = "12912861012"
    const val CLEUDDONIO = "09506382000"
    const val CATIANNA = "85733854057"
    const val CUTARDO = "58872160006"
    const val COANDRYA = "76179646090"
    const val GENIEVE = "98574307084"
    const val GODOPHREDO = "64065268052"
    const val GYANDULA = "23862179060"

    val EVENTS: List<SeedEventSpec> =
        listOf(
            criado("1291", CATHARYNA, GENIEVE, "2000-01-01", "2000-01-01T00:00:00"),
            movimento("1291", EventTypes.DEPOSITO, "1000.00", "2020-01-01T10:00:00"),
            movimento("1291", EventTypes.DEPOSITO, "900.00", "2020-01-01T11:00:00"),
            movimento("1291", EventTypes.SAQUE, "550.00", "2020-01-01T12:00:00"),
            movimento("1291", EventTypes.SAQUE, "350.00", "2020-01-01T13:00:00"),
            movimento("1291", EventTypes.DEPOSITO, "2000.00", "2020-01-10T15:00:00"),
            movimento("1291", EventTypes.SAQUE, "500.00", "2020-01-15T08:00:00"),
            transferencia(
                "1291",
                "0950",
                CATHARYNA,
                "Catharyna",
                CLEUDDONIO,
                "Cleuddônio",
                "1700.00",
                "2020-01-20T12:00:00",
            ),
            criado("0950", CLEUDDONIO, GODOPHREDO, "1990-10-10", "1990-10-10T00:00:00"),
            destino(
                "0950",
                "1291",
                CATHARYNA,
                "Catharyna",
                CLEUDDONIO,
                "Cleuddônio",
                "1700.00",
                "2020-01-20T12:00:00",
            ),
            movimento("0950", EventTypes.DEPOSITO, "1000.00", "2025-01-01T12:00:00"),
            movimento("0950", EventTypes.DEPOSITO, "5000.00", "2025-01-02T10:00:00"),
            movimento("0950", EventTypes.SAQUE, "200.00", "2025-01-10T10:00:00"),
            movimento("0950", EventTypes.DEPOSITO, "7000.00", "2025-02-05T10:00:00"),
            movimento("0950", EventTypes.SAQUE, "4500.00", "2025-03-06T11:00:00"),
            criado("8573", CATIANNA, GYANDULA, "2012-12-12", "2012-12-12T00:00:00"),
            movimento("8573", EventTypes.DEPOSITO, "1000.00", "2025-05-05T10:00:00"),
            movimento("8573", EventTypes.SAQUE, "800.00", "2025-05-06T10:00:00"),
            criado("5887", CUTARDO, GENIEVE, "2022-02-22", "2022-02-22T00:00:00"),
            movimento("5887", EventTypes.DEPOSITO, "150000.00", "2025-06-01T10:00:00"),
            criado("7617", COANDRYA, GODOPHREDO, "2025-01-01", "2025-01-01T00:00:00"),
            movimento("7617", EventTypes.DEPOSITO, "1500.00", "2025-07-01T10:00:00"),
        )

    fun toStored(specs: List<SeedEventSpec> = EVENTS): List<StoredEvent> {
        val versao = mutableMapOf<String, Int>()
        return specs.map { spec ->
            val next = versao.getOrDefault(spec.numero, 0) + 1
            versao[spec.numero] = next
            StoredEvent(
                id = UUID.nameUUIDFromBytes("conta:${spec.numero}:$next".toByteArray(StandardCharsets.UTF_8)),
                objetoId = spec.numero,
                tipo = spec.tipo,
                payload = spec.payload,
                versao = next,
                timestamp = DateTimes.parse(spec.timestamp),
            )
        }
    }

    private fun criado(
        numero: String,
        cpfCliente: String,
        cpfGerente: String,
        data: String,
        timestamp: String,
    ) = SeedEventSpec(
        numero,
        EventTypes.CRIADO,
        timestamp,
        mapOf(
            "cpfCliente" to cpfCliente,
            "cpfGerente" to cpfGerente,
            "saldoInicial" to "0.00",
            "dataCriacao" to data,
        ),
    )

    private fun movimento(
        numero: String,
        tipo: String,
        valor: String,
        timestamp: String,
    ) = SeedEventSpec(numero, tipo, timestamp, mapOf("valor" to valor))

    private fun transferencia(
        origem: String,
        destino: String,
        cpfOrigem: String,
        nomeOrigem: String,
        cpfDestino: String,
        nomeDestino: String,
        valor: String,
        timestamp: String,
    ) = SeedEventSpec(
        origem,
        EventTypes.TRANSFERENCIA_ORIGEM,
        timestamp,
        partes(origem, destino, cpfOrigem, nomeOrigem, cpfDestino, nomeDestino, valor),
    )

    private fun destino(
        destinoNumero: String,
        origem: String,
        cpfOrigem: String,
        nomeOrigem: String,
        cpfDestino: String,
        nomeDestino: String,
        valor: String,
        timestamp: String,
    ) = SeedEventSpec(
        destinoNumero,
        EventTypes.TRANSFERENCIA_DESTINO,
        timestamp,
        partes(origem, destinoNumero, cpfOrigem, nomeOrigem, cpfDestino, nomeDestino, valor),
    )

    private fun partes(
        origem: String,
        destino: String,
        cpfOrigem: String,
        nomeOrigem: String,
        cpfDestino: String,
        nomeDestino: String,
        valor: String,
    ) = mapOf(
        "valor" to valor,
        "origem" to mapOf("numeroConta" to origem, "cpf" to cpfOrigem, "nome" to nomeOrigem),
        "destino" to mapOf("numeroConta" to destino, "cpf" to cpfDestino, "nome" to nomeDestino),
    )
}
