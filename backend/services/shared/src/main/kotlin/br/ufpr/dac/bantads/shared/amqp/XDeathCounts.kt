package br.ufpr.dac.bantads.shared.amqp

object XDeathCounts {
    /** Falhas já contabilizadas em `x-death` a partir das quais a próxima vai para a DLQ (3 tentativas). */
    const val DLQ_AT = 2

    fun count(
        entries: List<*>?,
        queue: String,
    ): Long {
        if (entries == null) return 0
        return entries.sumOf { entry ->
            val map = entry as? Map<*, *> ?: return@sumOf 0L
            if (map["queue"]?.toString() != queue) return@sumOf 0L
            (map["count"] as? Number)?.toLong() ?: 0L
        }
    }
}
