package br.ufpr.dac.bantads.saga.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "saga")
data class SagaProperties(
    val stepTimeout: Duration = Duration.ofSeconds(30),
    val timeoutScanMs: Long = 1000,
    val sagaTtl: Duration = Duration.ofHours(1),
    val jobTtl: Duration = Duration.ofMinutes(5),
)
