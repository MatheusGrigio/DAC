package br.ufpr.dac.bantads.saga.job

data class JobRecord(
    val jobId: String,
    val status: String,
    val cpf: String? = null,
    val resultType: String? = null,
    val dominio: String? = null,
    val resourceId: String? = null,
    val resultado: Map<String, Any?>? = null,
    val erro: String? = null,
)
