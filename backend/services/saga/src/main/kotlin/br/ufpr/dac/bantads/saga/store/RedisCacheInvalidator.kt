package br.ufpr.dac.bantads.saga.store

import br.ufpr.dac.bantads.saga.engine.CacheInvalidator
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component

@Component
class RedisCacheInvalidator(
    private val redis: StringRedisTemplate,
) : CacheInvalidator {
    override fun deleteCliente(cpf: String?) = delete("cache:cliente:", cpf)

    override fun deleteGerente(cpf: String?) = delete("cache:gerente:", cpf)

    override fun deleteSessions(cpf: String?) {
        val id = cpf?.trim().orEmpty()
        if (id.isEmpty()) {
            return
        }
        val jti = redis.opsForValue().get("sessao:cpf:$id")
        redis.delete("sessao:cpf:$id")
        if (!jti.isNullOrBlank()) {
            redis.delete("sessao:$jti")
        }
    }

    private fun delete(
        prefix: String,
        cpf: String?,
    ) {
        val id = cpf?.trim().orEmpty()
        if (id.isNotEmpty()) {
            redis.delete("$prefix$id")
        }
    }
}
