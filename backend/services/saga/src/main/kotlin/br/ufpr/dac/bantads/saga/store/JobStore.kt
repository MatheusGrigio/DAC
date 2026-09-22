package br.ufpr.dac.bantads.saga.store

import br.ufpr.dac.bantads.saga.job.JobRecord

interface JobStore {
    fun find(jobId: String): JobRecord?

    fun save(job: JobRecord)
}
