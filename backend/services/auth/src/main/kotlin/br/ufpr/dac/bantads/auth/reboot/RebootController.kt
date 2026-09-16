package br.ufpr.dac.bantads.auth.reboot

import br.ufpr.dac.bantads.auth.dto.RebootResponse
import br.ufpr.dac.bantads.auth.saga.SagaInboxRepository
import br.ufpr.dac.bantads.auth.user.AuthService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class RebootController(
    private val authService: AuthService,
    private val sagaInbox: SagaInboxRepository,
) {
    @PostMapping("/internal/reboot")
    fun reboot(): RebootResponse {
        sagaInbox.deleteAll()
        val count = authService.reboot()
        return RebootResponse(usuarios = count)
    }
}
