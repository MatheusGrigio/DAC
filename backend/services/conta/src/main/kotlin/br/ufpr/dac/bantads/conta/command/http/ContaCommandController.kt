package br.ufpr.dac.bantads.conta.command.http

import jakarta.validation.Valid
import jakarta.validation.constraints.Pattern
import org.springframework.hateoas.EntityModel
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/contas")
class ContaCommandController(
    private val service: ContaCommandService,
    private val assembler: OperacaoAssembler,
) {
    @PostMapping("/{numero}/deposito")
    fun depositar(
        @PathVariable @Pattern(regexp = "^\\d{4}$") numero: String,
        @Valid @RequestBody body: OperacaoInput,
        @RequestHeader("X-User-CPF") userCpf: String,
        @RequestHeader("X-User-Tipo") userTipo: String,
    ): ResponseEntity<EntityModel<OperacaoRealizadaView>> {
        val view = service.depositar(numero, body.valor, userCpf, userTipo)
        return ResponseEntity.status(HttpStatus.CREATED).body(assembler.model(view))
    }

    @PostMapping("/{numero}/saque")
    fun sacar(
        @PathVariable @Pattern(regexp = "^\\d{4}$") numero: String,
        @Valid @RequestBody body: OperacaoInput,
        @RequestHeader("X-User-CPF") userCpf: String,
        @RequestHeader("X-User-Tipo") userTipo: String,
    ): ResponseEntity<EntityModel<OperacaoRealizadaView>> {
        val view = service.sacar(numero, body.valor, userCpf, userTipo)
        return ResponseEntity.status(HttpStatus.CREATED).body(assembler.model(view))
    }

    @PostMapping("/{numero}/transferencia")
    fun transferir(
        @PathVariable @Pattern(regexp = "^\\d{4}$") numero: String,
        @Valid @RequestBody body: TransferenciaCommandInput,
        @RequestHeader("X-User-CPF") userCpf: String,
        @RequestHeader("X-User-Tipo") userTipo: String,
    ): ResponseEntity<EntityModel<OperacaoRealizadaView>> {
        val view = service.transferir(numero, body, userCpf, userTipo)
        return ResponseEntity.status(HttpStatus.CREATED).body(assembler.model(view))
    }
}
