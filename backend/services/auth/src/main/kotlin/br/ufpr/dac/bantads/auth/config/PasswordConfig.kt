package br.ufpr.dac.bantads.auth.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

@Configuration
class PasswordConfig(
    @Value("\${argon2.salt-length}") private val saltLength: Int,
    @Value("\${argon2.hash-length}") private val hashLength: Int,
    @Value("\${argon2.parallelism}") private val parallelism: Int,
    @Value("\${argon2.memory-kb}") private val memoryKb: Int,
    @Value("\${argon2.iterations}") private val iterations: Int,
) {
    @Bean
    fun passwordEncoder(): PasswordEncoder = Argon2PasswordEncoder(saltLength, hashLength, parallelism, memoryKb, iterations)
}
