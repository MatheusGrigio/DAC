package br.ufpr.dac.bantads.email.health

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class HealthController {

    @GetMapping("/health")
    fun health(): ResponseEntity<String> {
        return ResponseEntity.ok("OK")
    }
}
