package br.ufpr.dac.bantads.shared.error

import br.ufpr.dac.bantads.shared.json.BantadsJson
import kotlin.test.Test
import kotlin.test.assertEquals

class ErroBodyJsonTest {
    private val mapper = BantadsJson.mapper()

    @Test
    fun `erro body matches swagger required fields`() {
        val body = ErroBody.unprocessable("Saldo insuficiente para a operação")
        val json = mapper.writeValueAsString(body)
        val tree = mapper.readTree(json)
        assertEquals(422, tree["status"].asInt())
        assertEquals("Unprocessable Entity", tree["erro"].asText())
        assertEquals("Saldo insuficiente para a operação", tree["mensagem"].asText())
    }

    @Test
    fun `auth error messages are exact`() {
        val json = mapper.writeValueAsString(AuthErrorBody(message = AuthMessages.TOKEN_AUSENTE))
        val tree = mapper.readTree(json)
        assertEquals(false, tree["auth"].asBoolean())
        assertEquals("Token não fornecido.", tree["message"].asText())
        assertEquals("Falha ao autenticar o token.", AuthMessages.TOKEN_INVALIDO)
        assertEquals("Login inválido!", AuthMessages.LOGIN_INVALIDO)
    }
}
