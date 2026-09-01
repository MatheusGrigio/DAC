package br.ufpr.dac.bantads.cliente.health

import br.ufpr.dac.bantads.shared.health.HealthResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class HealthController {
    @GetMapping("/health")
    fun health(): HealthResponse = HealthResponse()
}
