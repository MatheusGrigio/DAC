package br.ufpr.dac.bantads.shared.text

import java.text.Normalizer
import java.util.Locale

/** Same idea as Gateway `Intl.Collator('pt-BR', { sensitivity: 'base' })`. */
object PtBrNames {
    private val combining = Regex("\\p{M}+")

    fun fold(nome: String): String =
        Normalizer
            .normalize(nome, Normalizer.Form.NFD)
            .replace(combining, "")
            .lowercase(Locale.ROOT)

    fun compare(
        left: String,
        right: String,
    ): Int = fold(left).compareTo(fold(right))

    fun sort(nomes: List<String>): List<String> = nomes.sortedWith(::compare)
}
