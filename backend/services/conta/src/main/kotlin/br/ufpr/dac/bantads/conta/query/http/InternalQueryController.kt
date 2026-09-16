package br.ufpr.dac.bantads.conta.query.http

import br.ufpr.dac.bantads.conta.web.Identity
import jakarta.validation.constraints.Pattern
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/internal")
class InternalQueryController(
    private val service: ContaQueryService,
) {
    @GetMapping("/saldos")
    fun saldos(
        @RequestHeader("X-User-CPF") userCpf: String,
        @RequestHeader("X-User-Tipo") userTipo: String,
    ): Map<String, SaldoInternoView> {
        Identity.requireGerente(userTipo)
        return service.saldos()
    }

    @GetMapping("/contagem-por-gerente")
    fun contagem(
        @RequestHeader("X-User-CPF") userCpf: String,
        @RequestHeader("X-User-Tipo") userTipo: String,
    ): Map<String, Int> {
        Identity.requireGerente(userTipo)
        return service.contagemPorGerente()
    }

    @GetMapping("/contas/{numero}")
    fun conta(
        @PathVariable @Pattern(regexp = "^\\d{4}$") numero: String,
        @RequestHeader("X-User-CPF") userCpf: String,
        @RequestHeader("X-User-Tipo") userTipo: String,
    ): InternalContaView {
        Identity.requireAuthenticated(userTipo)
        return service.obterInterno(numero)
    }
}
