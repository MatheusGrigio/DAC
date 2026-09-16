package br.ufpr.dac.bantads.gerente.cadastro

import br.ufpr.dac.bantads.gerente.dto.GerenteUpdate
import br.ufpr.dac.bantads.gerente.hateoas.GerenteAssembler
import br.ufpr.dac.bantads.gerente.hateoas.GerentesListModel
import br.ufpr.dac.bantads.gerente.web.Identity
import jakarta.validation.Valid
import org.springframework.hateoas.EntityModel
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/gerentes")
class GerenteController(
    private val service: GerenteService,
    private val assembler: GerenteAssembler,
) {
    @GetMapping
    fun listar(
        @RequestHeader("X-User-CPF") userCpf: String,
        @RequestHeader("X-User-Tipo") userTipo: String,
    ): GerentesListModel {
        Identity.requireGerente(userTipo)
        return assembler.gerentes(service.listarAtivos(), userCpf)
    }

    @GetMapping("/{cpf}")
    fun obter(
        @PathVariable cpf: String,
        @RequestHeader("X-User-CPF") userCpf: String,
        @RequestHeader("X-User-Tipo") userTipo: String,
    ): EntityModel<*> {
        Identity.requireGerente(userTipo)
        return assembler.gerente(service.obter(cpf), userCpf)
    }

    @PutMapping("/{cpf}")
    fun atualizar(
        @PathVariable cpf: String,
        @Valid @RequestBody body: GerenteUpdate,
        @RequestHeader("X-User-CPF") userCpf: String,
        @RequestHeader("X-User-Tipo") userTipo: String,
    ): EntityModel<*> {
        Identity.requireGerente(userTipo)
        return assembler.gerente(service.atualizar(cpf, body), userCpf)
    }
}
