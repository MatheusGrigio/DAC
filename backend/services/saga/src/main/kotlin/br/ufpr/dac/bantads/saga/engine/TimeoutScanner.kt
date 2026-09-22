package br.ufpr.dac.bantads.saga.engine

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class TimeoutScanner(
    private val engine: SagaEngine,
) {
    @Scheduled(fixedDelayString = "\${saga.timeout-scan-ms:1000}")
    fun scan() {
        engine.failTimedOutSteps()
    }
}
