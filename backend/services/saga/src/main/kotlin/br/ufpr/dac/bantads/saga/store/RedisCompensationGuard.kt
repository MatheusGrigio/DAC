package br.ufpr.dac.bantads.saga.store

import br.ufpr.dac.bantads.saga.config.SagaProperties
import br.ufpr.dac.bantads.saga.engine.CompensationGuard
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component

@Component
class RedisCompensationGuard(
    private val redis: StringRedisTemplate,
    private val properties: SagaProperties,
) : CompensationGuard {
    override fun tryAcquire(
        sagaId: String,
        etapa: Int,
    ): Boolean {
        val ok = redis.opsForValue().setIfAbsent(key(sagaId, etapa), "1", properties.sagaTtl)
        return ok == true
    }

    companion object {
        fun key(
            sagaId: String,
            etapa: Int,
        ): String = "saga:$sagaId:lock:comp:$etapa"
    }
}
