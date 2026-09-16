package br.ufpr.dac.bantads.auth.password

import java.security.SecureRandom

object RandomPassword {
    private val alphabet = (('a'..'z') + ('A'..'Z') + ('0'..'9')).toCharArray()
    private val random = SecureRandom()

    fun generate(length: Int = 8): String {
        require(length > 0)
        return CharArray(length) { alphabet[random.nextInt(alphabet.size)] }.concatToString()
    }
}
