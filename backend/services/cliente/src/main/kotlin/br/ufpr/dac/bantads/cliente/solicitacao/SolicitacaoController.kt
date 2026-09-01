package br.ufpr.dac.bantads.cliente.solicitacao

import br.ufpr.dac.bantads.cliente.dto.AutocadastroInput
import br.ufpr.dac.bantads.cliente.dto.RejeicaoInput
import br.ufpr.dac.bantads.cliente.hateoas.ClienteAssembler
import br.ufpr.dac.bantads.cliente.hateoas.SolicitacoesListModel
import br.ufpr.dac.bantads.cliente.web.Identity
import jakarta.validation.Valid
import org.springframework.hateoas.EntityModel
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@RestController
@RequestMapping("/solicitacoes")
class SolicitacaoController(
    private val service: SolicitacaoService,
    private val assembler: ClienteAssembler,
) {
    @PostMapping
    fun criar(
        @Valid @RequestBody body: AutocadastroInput,
    ): ResponseEntity<EntityModel<*>> {
        val saved = service.criar(body)
        val model = assembler.solicitacao(saved)
        return ResponseEntity.created(URI.create("/solicitacoes/${saved.cpf}")).body(model)
    }

    @GetMapping
    fun listar(
        @RequestParam(required = false) status: StatusSolicitacao?,
        @RequestHeader("X-User-CPF") userCpf: String,
        @RequestHeader("X-User-Tipo") userTipo: String,
    ): SolicitacoesListModel {
        Identity.requireGerente(userTipo)
        return assembler.solicitacoes(service.listar(status))
    }

    @GetMapping("/{cpf}")
    fun obter(
        @PathVariable cpf: String,
        @RequestHeader("X-User-CPF") userCpf: String,
        @RequestHeader("X-User-Tipo") userTipo: String,
    ): EntityModel<*> {
        Identity.requireGerente(userTipo)
        return assembler.solicitacao(service.obter(cpf))
    }

    @PostMapping("/{cpf}/rejeicao")
    fun rejeitar(
        @PathVariable cpf: String,
        @Valid @RequestBody body: RejeicaoInput,
        @RequestHeader("X-User-CPF") userCpf: String,
        @RequestHeader("X-User-Tipo") userTipo: String,
    ): EntityModel<*> {
        Identity.requireGerente(userTipo)
        return assembler.solicitacao(service.rejeitar(cpf, body.motivo))
    }
}
