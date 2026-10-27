package br.ufpr.dac.bantads.email.mail

data class Destinatario(
    val email: String,
    val nome: String?,
)

data class OutgoingMail(
    val to: String,
    val subject: String,
    val body: String,
    val senha: String? = null,
)

interface MailSender {
    fun send(mail: OutgoingMail): String
}
