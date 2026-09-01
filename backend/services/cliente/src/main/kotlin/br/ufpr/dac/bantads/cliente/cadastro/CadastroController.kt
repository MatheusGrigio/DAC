package br.ufpr.dac.bantads.cliente.cadastro

import br.ufpr.dac.bantads.cliente.dto.ClienteNomeView
import br.ufpr.dac.bantads.cliente.hateoas.ClienteAssembler
import br.ufpr.dac.bantads.cliente.hateoas.ClientesListModel
import br.ufpr.dac.bantads.cliente.web.Identity
import org.springframework.hateoas.EntityModel
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/clientes")
class CadastroController(
    private val service: CadastroService,
    private val assembler: ClienteAssembler,
) {
    @GetMapping
    fun listar(
        @RequestParam(required = false) busca: String?,
        @RequestHeader("X-User-CPF") userCpf: String,
        @RequestHeader("X-User-Tipo") userTipo: String,
    ): ClientesListModel {
        Identity.requireGerente(userTipo)
        return assembler.clientes(service.buscar(busca))
    }

    @GetMapping("/nomes")
    fun nomes(
        @RequestParam(required = false) cpfs: String?,
        @RequestHeader("X-User-CPF") userCpf: String,
        @RequestHeader("X-User-Tipo") userTipo: String,
    ): Map<String, List<ClienteNomeView>> {
        Identity.requireAuthenticated(userTipo)
        val lista =
            cpfs
                .orEmpty()
                .split(',')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        return mapOf("clientes" to service.nomesPorCpfs(lista))
    }

    @GetMapping("/{cpf}")
    fun obter(
        @PathVariable cpf: String,
        @RequestHeader("X-User-CPF") userCpf: String,
        @RequestHeader("X-User-Tipo") userTipo: String,
    ): EntityModel<*> {
        Identity.requireGerenteOrSelf(userTipo, userCpf, cpf)
        return assembler.cliente(service.obter(cpf))
    }
}
