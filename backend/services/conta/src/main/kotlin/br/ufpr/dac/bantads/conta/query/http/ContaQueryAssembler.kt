package br.ufpr.dac.bantads.conta.query.http

import br.ufpr.dac.bantads.shared.domain.Perfil
import org.springframework.hateoas.EntityModel
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo
import org.springframework.stereotype.Component

@Component
class ContaQueryAssembler {
    fun conta(
        view: ContaView,
        userTipo: String,
    ): EntityModel<ContaView> {
        val model = EntityModel.of(view)
        val conta = linkTo(ContaQueryController::class.java).slash(view.numero)
        model.add(conta.withSelfRel())
        model.add(linkTo(ClienteContaQueryController::class.java).slash(view.cpfCliente).withRel("cliente"))
        if (userTipo == Perfil.CLIENTE.wire) {
            model.add(conta.slash("deposito").withRel("deposito"))
            model.add(conta.slash("saque").withRel("saque"))
            model.add(conta.slash("transferencia").withRel("transferencia"))
            model.add(conta.slash("extrato").withRel("extrato"))
        }
        return model
    }

    fun extrato(view: ExtratoView): EntityModel<ExtratoView> {
        val model = EntityModel.of(view)
        val conta = linkTo(ContaQueryController::class.java).slash(view.numeroConta)
        model.add(conta.slash("extrato").withSelfRel())
        model.add(conta.withRel("conta"))
        return model
    }
}
