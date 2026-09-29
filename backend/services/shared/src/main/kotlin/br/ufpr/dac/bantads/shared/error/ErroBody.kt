package br.ufpr.dac.bantads.shared.error

data class ErroBody(
    val status: Int,
    val erro: String,
    val mensagem: String,
) {
    companion object {
        fun badRequest(mensagem: String) = ErroBody(400, ErroNomes.BAD_REQUEST, mensagem)

        fun forbidden(mensagem: String) = ErroBody(403, ErroNomes.FORBIDDEN, mensagem)

        fun notFound(mensagem: String) = ErroBody(404, ErroNomes.NOT_FOUND, mensagem)

        fun conflict(mensagem: String) = ErroBody(409, ErroNomes.CONFLICT, mensagem)

        fun unprocessable(mensagem: String) = ErroBody(422, ErroNomes.UNPROCESSABLE, mensagem)
    }
}

object ErroNomes {
    const val BAD_REQUEST = "Bad Request"
    const val FORBIDDEN = "Forbidden"
    const val NOT_FOUND = "Not Found"
    const val CONFLICT = "Conflict"
    const val UNPROCESSABLE = "Unprocessable Entity"
}
