package br.ufpr.dac.bantads.auth.health

import br.ufpr.dac.bantads.shared.health.HealthResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class HealthController {
    @GetMapping("/health", "/auth/health")
    fun health(): HealthResponse = HealthResponse()
}
