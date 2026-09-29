package br.ufpr.dac.bantads.shared.money

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import java.math.BigDecimal

class MoneyDeserializer : JsonDeserializer<BigDecimal>() {
    override fun deserialize(
        parser: JsonParser,
        ctxt: DeserializationContext,
    ): BigDecimal {
        val raw = parser.valueAsString ?: error("Money value must be a JSON string")
        return Money.parse(raw)
    }
}
