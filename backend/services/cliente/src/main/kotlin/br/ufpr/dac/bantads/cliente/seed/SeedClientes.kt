package br.ufpr.dac.bantads.cliente.seed

import br.ufpr.dac.bantads.shared.money.Money
import java.math.BigDecimal

data class SeedCliente(
    val cpf: String,
    val nome: String,
    val email: String,
    val telefone: String,
    val salario: BigDecimal,
    val logradouro: String,
    val numero: String,
    val cep: String,
    val cidade: String,
    val uf: String,
)

object SeedClientes {
    val ALL: List<SeedCliente> =
        listOf(
            seed(
                "12912861012",
                "Catharyna",
                "cli1@bantads.com.br",
                "41999990001",
                "10000.00",
                "Rua XV de Novembro",
                "1299",
                "80060000",
            ),
            seed(
                "09506382000",
                "Cleuddônio",
                "cli2@bantads.com.br",
                "41999990002",
                "20000.00",
                "Rua Marechal Deodoro",
                "630",
                "80010010",
            ),
            seed(
                "85733854057",
                "Catianna",
                "cli3@bantads.com.br",
                "41999990003",
                "3000.00",
                "Avenida Sete de Setembro",
                "2775",
                "80230010",
            ),
            seed(
                "58872160006",
                "Cutardo",
                "cli4@bantads.com.br",
                "41999990004",
                "500.00",
                "Rua Comendador Araujo",
                "143",
                "80420000",
            ),
            seed(
                "76179646090",
                "Coândrya",
                "cli5@bantads.com.br",
                "41999990005",
                "1500.00",
                "Rua Emiliano Perneta",
                "390",
                "80420080",
            ),
        )

    private fun seed(
        cpf: String,
        nome: String,
        email: String,
        telefone: String,
        salario: String,
        logradouro: String,
        numero: String,
        cep: String,
        cidade: String = "Curitiba",
        uf: String = "PR",
    ) = SeedCliente(
        cpf = cpf,
        nome = nome,
        email = email,
        telefone = telefone,
        salario = Money.parse(salario),
        logradouro = logradouro,
        numero = numero,
        cep = cep,
        cidade = cidade,
        uf = uf,
    )
}
