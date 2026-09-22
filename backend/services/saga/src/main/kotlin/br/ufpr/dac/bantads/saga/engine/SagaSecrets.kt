package br.ufpr.dac.bantads.saga.engine

import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class SagaSecrets {
    private val senhas = ConcurrentHashMap<String, String>()

    fun put(
        sagaId: String,
        senha: String,
    ) {
        senhas[sagaId] = senha
    }

    fun take(sagaId: String): String? = senhas.remove(sagaId)

    fun peek(sagaId: String): String? = senhas[sagaId]

    fun clear(sagaId: String) {
        senhas.remove(sagaId)
    }
}
