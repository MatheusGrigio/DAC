package br.ufpr.dac.bantads.saga.store

import br.ufpr.dac.bantads.saga.config.SagaProperties
import br.ufpr.dac.bantads.saga.engine.SagaState
import br.ufpr.dac.bantads.saga.engine.SagaStatuses
import br.ufpr.dac.bantads.shared.json.BantadsJson
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Repository

@Repository
class RedisSagaStateStore(
    private val redis: StringRedisTemplate,
    private val properties: SagaProperties,
) : SagaStateStore {
    private val mapper = BantadsJson.mapper()

    override fun find(sagaId: String): SagaState? {
        val json = redis.opsForValue().get(key(sagaId)) ?: return null
        return mapper.readValue(json, SagaState::class.java)
    }

    override fun save(state: SagaState) {
        redis.opsForValue().set(key(state.sagaId), mapper.writeValueAsString(state), properties.sagaTtl)
        if (state.status == SagaStatuses.EM_ANDAMENTO) {
            redis.opsForSet().add(ACTIVE, state.sagaId)
            redis.expire(ACTIVE, properties.sagaTtl)
        } else {
            redis.opsForSet().remove(ACTIVE, state.sagaId)
        }
    }

    override fun findInProgress(): List<SagaState> {
        val ids = redis.opsForSet().members(ACTIVE) ?: emptySet()
        return ids
            .mapNotNull { find(it) }
            .filter { it.status == SagaStatuses.EM_ANDAMENTO && it.timeoutAtEpochMs != null }
    }

    companion object {
        const val ACTIVE = "saga:active"

        fun key(sagaId: String): String = "saga:$sagaId"
    }
}
