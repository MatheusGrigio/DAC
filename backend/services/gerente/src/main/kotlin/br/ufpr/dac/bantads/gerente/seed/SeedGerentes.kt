package br.ufpr.dac.bantads.gerente.seed

data class SeedGerente(
    val cpf: String,
    val nome: String,
    val email: String,
    val telefone: String,
)

object SeedGerentes {
    val ALL: List<SeedGerente> =
        listOf(
            SeedGerente("98574307084", "Geniéve", "ger1@bantads.com.br", "41988880001"),
            SeedGerente("64065268052", "Godophredo", "ger2@bantads.com.br", "41988880002"),
            SeedGerente("23862179060", "Gyândula", "ger3@bantads.com.br", "41988880003"),
            SeedGerente("40501740066", "Gadamântio", "ger4@bantads.com.br", "41988880004"),
        )
}
