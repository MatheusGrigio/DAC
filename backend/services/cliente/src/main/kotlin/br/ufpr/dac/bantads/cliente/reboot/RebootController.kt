package br.ufpr.dac.bantads.cliente.reboot

import br.ufpr.dac.bantads.cliente.cadastro.CadastroService
import br.ufpr.dac.bantads.cliente.dto.RebootResponse
import br.ufpr.dac.bantads.cliente.saga.SagaInboxRepository
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class RebootController(
    private val cadastro: CadastroService,
    private val sagaInbox: SagaInboxRepository,
) {
    @PostMapping("/internal/reboot")
    fun reboot(): RebootResponse {
        sagaInbox.deleteAll()
        return RebootResponse(clientes = cadastro.reboot())
    }
}
