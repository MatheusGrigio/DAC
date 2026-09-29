package br.ufpr.dac.bantads.shared.money

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Money at the API boundary: JSON string `^\d+\.\d{2}$` (e.g. `"800.00"`).
 * PostgreSQL stores NUMERIC(19,4); format back to 2 decimal places.
 */
object Money {
    private val pattern = Regex("""^\d+\.\d{2}$""")
    private const val SCALE = 2
    private val rounding = RoundingMode.HALF_EVEN

    fun parse(value: String): BigDecimal {
        require(pattern.matches(value)) { "Invalid money format: $value" }
        return BigDecimal(value).setScale(SCALE, rounding)
    }

    fun format(value: BigDecimal): String = value.setScale(SCALE, rounding).toPlainString()

    fun add(
        left: BigDecimal,
        right: BigDecimal,
    ): BigDecimal = left.add(right).setScale(SCALE, rounding)

    fun subtract(
        left: BigDecimal,
        right: BigDecimal,
    ): BigDecimal = left.subtract(right).setScale(SCALE, rounding)

    fun isPositive(value: BigDecimal): Boolean = value.signum() > 0

    fun gte(
        left: BigDecimal,
        right: BigDecimal,
    ): Boolean = left.compareTo(right) >= 0
}
