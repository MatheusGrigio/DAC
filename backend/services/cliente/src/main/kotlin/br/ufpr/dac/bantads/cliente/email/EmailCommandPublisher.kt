package br.ufpr.dac.bantads.cliente.email

interface EmailCommandPublisher {
    fun publishRejeicao(
        email: String,
        nome: String,
        motivo: String,
    )
}
