package br.ufpr.dac.bantads.gerente.hateoas

import br.ufpr.dac.bantads.gerente.cadastro.GerenteController
import br.ufpr.dac.bantads.gerente.cadastro.GerenteEntity
import br.ufpr.dac.bantads.gerente.dto.GerenteView
import org.springframework.hateoas.EntityModel
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo
import org.springframework.stereotype.Component

class GerentesListModel(
    val gerentes: List<EntityModel<GerenteView>>,
) : RepresentationModel<GerentesListModel>()

@Component
class GerenteAssembler {
    fun gerente(
        entity: GerenteEntity,
        userCpf: String,
    ): EntityModel<GerenteView> {
        val model = EntityModel.of(toView(entity))
        val self = linkTo(GerenteController::class.java).slash(entity.cpf)
        model.add(self.withSelfRel())
        if (entity.ativo) {
            model.add(self.withRel("atualizacao"))
            if (userCpf != entity.cpf) {
                model.add(self.withRel("remocao"))
            }
        }
        return model
    }

    fun gerentes(
        items: List<GerenteEntity>,
        userCpf: String,
    ): GerentesListModel {
        val list = GerentesListModel(items.map { gerente(it, userCpf) })
        list.add(linkTo(GerenteController::class.java).withSelfRel())
        list.add(linkTo(GerenteController::class.java).withRel("criacao"))
        return list
    }

    fun toView(entity: GerenteEntity) =
        GerenteView(
            cpf = entity.cpf,
            nome = entity.nome,
            email = entity.email,
            telefone = entity.telefone,
            ativo = entity.ativo,
            quantidadeClientes = null,
        )
}
