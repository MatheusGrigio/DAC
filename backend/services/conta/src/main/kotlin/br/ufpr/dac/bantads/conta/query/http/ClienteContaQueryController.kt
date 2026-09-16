package br.ufpr.dac.bantads.conta.query.http

import jakarta.validation.constraints.Pattern
import org.springframework.hateoas.EntityModel
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/clientes")
class ClienteContaQueryController(
    private val service: ContaQueryService,
    private val assembler: ContaQueryAssembler,
) {
    @GetMapping("/{cpf}/conta")
    fun obter(
        @PathVariable @Pattern(regexp = "^\\d{11}$") cpf: String,
        @RequestHeader("X-User-CPF") userCpf: String,
        @RequestHeader("X-User-Tipo") userTipo: String,
    ): EntityModel<ContaView> = assembler.conta(service.obterPorCpf(cpf, userCpf, userTipo), userTipo)
}
