package br.ufpr.dac.bantads.conta.query.http

import jakarta.validation.constraints.Pattern
import org.springframework.hateoas.EntityModel
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@Validated
@RestController
@RequestMapping("/contas")
class ContaQueryController(
    private val service: ContaQueryService,
    private val assembler: ContaQueryAssembler,
) {
    @GetMapping("/{numero}")
    fun obter(
        @PathVariable @Pattern(regexp = "^\\d{4}$") numero: String,
        @RequestHeader("X-User-CPF") userCpf: String,
        @RequestHeader("X-User-Tipo") userTipo: String,
    ): EntityModel<ContaView> = assembler.conta(service.obterPorNumero(numero, userCpf, userTipo), userTipo)

    @GetMapping("/{numero}/extrato")
    fun extrato(
        @PathVariable @Pattern(regexp = "^\\d{4}$") numero: String,
        @RequestParam(required = false) inicio: LocalDate?,
        @RequestParam(required = false) fim: LocalDate?,
        @RequestHeader("X-User-CPF") userCpf: String,
        @RequestHeader("X-User-Tipo") userTipo: String,
    ): EntityModel<ExtratoView> = assembler.extrato(service.extrato(numero, inicio, fim, userCpf, userTipo))
}
