package br.ufpr.dac.bantads.auth.seed

import br.ufpr.dac.bantads.shared.domain.Perfil

data class SeedUser(
    val cpf: String,
    val login: String,
    val tipo: String,
)

object SeedUsers {
    const val SENHA = "tads"

    val ALL: List<SeedUser> =
        listOf(
            SeedUser("12912861012", "cli1@bantads.com.br", Perfil.CLIENTE.wire),
            SeedUser("09506382000", "cli2@bantads.com.br", Perfil.CLIENTE.wire),
            SeedUser("85733854057", "cli3@bantads.com.br", Perfil.CLIENTE.wire),
            SeedUser("58872160006", "cli4@bantads.com.br", Perfil.CLIENTE.wire),
            SeedUser("76179646090", "cli5@bantads.com.br", Perfil.CLIENTE.wire),
            SeedUser("98574307084", "ger1@bantads.com.br", Perfil.GERENTE.wire),
            SeedUser("64065268052", "ger2@bantads.com.br", Perfil.GERENTE.wire),
            SeedUser("23862179060", "ger3@bantads.com.br", Perfil.GERENTE.wire),
            SeedUser("40501740066", "ger4@bantads.com.br", Perfil.GERENTE.wire),
        )
}
