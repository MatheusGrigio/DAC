package br.ufpr.dac.bantads.gerente.reboot

import br.ufpr.dac.bantads.gerente.cadastro.GerenteService
import br.ufpr.dac.bantads.gerente.dto.RebootResponse
import br.ufpr.dac.bantads.gerente.saga.SagaInboxRepository
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class RebootController(
    private val gerentes: GerenteService,
    private val sagaInbox: SagaInboxRepository,
) {
    @PostMapping("/internal/reboot")
    fun reboot(): RebootResponse {
        sagaInbox.deleteAll()
        return RebootResponse(gerentes = gerentes.reboot())
    }
}
