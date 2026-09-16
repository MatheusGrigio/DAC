package br.ufpr.dac.bantads.conta.command.numbering

import kotlin.random.Random

object AccountNumberGenerator {
    fun generate(
        cpfCliente: String,
        exists: (String) -> Boolean,
        random: Random = Random.Default,
    ): String {
        val prefixoCpf = cpfCliente.take(4)
        repeat(10_000) {
            val numero = random.nextInt(0, 10_000).toString().padStart(4, '0')
            if (numero != prefixoCpf && !exists(numero)) return numero
        }
        error("Não foi possível gerar número de conta livre")
    }
}
