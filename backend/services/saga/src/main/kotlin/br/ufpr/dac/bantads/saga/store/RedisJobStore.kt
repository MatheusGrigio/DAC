package br.ufpr.dac.bantads.saga.store

import br.ufpr.dac.bantads.saga.config.SagaProperties
import br.ufpr.dac.bantads.saga.job.JobRecord
import br.ufpr.dac.bantads.shared.json.BantadsJson
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Repository

@Repository
class RedisJobStore(
    private val redis: StringRedisTemplate,
    private val properties: SagaProperties,
) : JobStore {
    private val mapper = BantadsJson.mapper()

    override fun find(jobId: String): JobRecord? {
        val json = redis.opsForValue().get(key(jobId)) ?: return null
        return mapper.readValue(json, JobRecord::class.java)
    }

    override fun save(job: JobRecord) {
        redis.opsForValue().set(key(job.jobId), mapper.writeValueAsString(job), properties.jobTtl)
    }

    companion object {
        fun key(jobId: String): String = "job:$jobId"
    }
}
