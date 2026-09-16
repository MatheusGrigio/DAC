package br.ufpr.dac.bantads.conta.reboot

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class RebootController(
    private val reboot: RebootService,
) {
    @PostMapping("/internal/reboot")
    fun reboot(): RebootResponse = reboot.reboot()
}
