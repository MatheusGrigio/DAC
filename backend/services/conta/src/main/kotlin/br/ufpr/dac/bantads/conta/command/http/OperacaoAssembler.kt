package br.ufpr.dac.bantads.conta.command.http

import org.springframework.hateoas.EntityModel
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo
import org.springframework.stereotype.Component

@Component
class OperacaoAssembler {
    fun model(view: OperacaoRealizadaView): EntityModel<OperacaoRealizadaView> {
        val model = EntityModel.of(view)
        val conta = linkTo(ContaCommandController::class.java).slash(view.numeroConta)
        model.add(conta.withRel("conta"))
        model.add(conta.slash("extrato").withRel("extrato"))
        return model
    }
}
