package br.ufpr.dac.bantads.shared.time

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object DateTimes {
    val ZONE: ZoneId = ZoneId.of("America/Sao_Paulo")
    val FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
    private val pattern = Regex("""^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}$""")

    fun now(): String = format(LocalDateTime.now(ZONE))

    fun format(value: LocalDateTime): String = value.format(FORMATTER)

    fun parse(value: String): LocalDateTime {
        require(pattern.matches(value)) { "Invalid timestamp: $value" }
        return LocalDateTime.parse(value, FORMATTER)
    }
}
