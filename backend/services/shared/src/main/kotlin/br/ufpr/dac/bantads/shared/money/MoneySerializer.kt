package br.ufpr.dac.bantads.shared.money

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import java.math.BigDecimal

class MoneySerializer : JsonSerializer<BigDecimal>() {
    override fun serialize(
        value: BigDecimal,
        gen: JsonGenerator,
        serializers: SerializerProvider,
    ) {
        gen.writeString(Money.format(value))
    }
}
