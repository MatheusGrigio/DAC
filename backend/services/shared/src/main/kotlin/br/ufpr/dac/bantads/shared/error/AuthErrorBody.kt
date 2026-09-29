package br.ufpr.dac.bantads.shared.error

data class AuthErrorBody(
    val auth: Boolean = false,
    val message: String,
)

object AuthMessages {
    const val TOKEN_AUSENTE = "Token não fornecido."
    const val TOKEN_INVALIDO = "Falha ao autenticar o token."
    const val LOGIN_INVALIDO = "Login inválido!"
}
