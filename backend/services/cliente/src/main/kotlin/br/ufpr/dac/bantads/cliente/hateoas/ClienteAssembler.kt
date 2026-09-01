package br.ufpr.dac.bantads.cliente.hateoas

import br.ufpr.dac.bantads.cliente.cadastro.CadastroController
import br.ufpr.dac.bantads.cliente.cadastro.ClienteEntity
import br.ufpr.dac.bantads.cliente.domain.EnderecoEmbeddable
import br.ufpr.dac.bantads.cliente.dto.ClienteView
import br.ufpr.dac.bantads.cliente.dto.EnderecoView
import br.ufpr.dac.bantads.cliente.dto.SolicitacaoView
import br.ufpr.dac.bantads.cliente.solicitacao.SolicitacaoController
import br.ufpr.dac.bantads.cliente.solicitacao.SolicitacaoEntity
import br.ufpr.dac.bantads.cliente.solicitacao.StatusSolicitacao
import org.springframework.hateoas.EntityModel
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo
import org.springframework.stereotype.Component

class SolicitacoesListModel(
    val solicitacoes: List<EntityModel<SolicitacaoView>>,
) : RepresentationModel<SolicitacoesListModel>()

class ClientesListModel(
    val clientes: List<EntityModel<ClienteView>>,
) : RepresentationModel<ClientesListModel>()

@Component
class ClienteAssembler {
    fun solicitacao(entity: SolicitacaoEntity): EntityModel<SolicitacaoView> {
        val model = EntityModel.of(toView(entity))
        model.add(linkTo(SolicitacaoController::class.java).slash(entity.cpf).withSelfRel())
        if (entity.status == StatusSolicitacao.PENDENTE) {
            val base = linkTo(SolicitacaoController::class.java).slash(entity.cpf)
            model.add(base.slash("aprovacao").withRel("aprovacao"))
            model.add(base.slash("rejeicao").withRel("rejeicao"))
        }
        return model
    }

    fun solicitacoes(items: List<SolicitacaoEntity>): SolicitacoesListModel {
        val list = SolicitacoesListModel(items.map { solicitacao(it) })
        list.add(linkTo(SolicitacaoController::class.java).withSelfRel())
        return list
    }

    fun cliente(entity: ClienteEntity): EntityModel<ClienteView> {
        val model = EntityModel.of(toView(entity))
        model.add(linkTo(CadastroController::class.java).slash(entity.cpf).withSelfRel())
        model.add(linkTo(CadastroController::class.java).slash(entity.cpf).slash("conta").withRel("conta"))
        return model
    }

    fun clientes(items: List<ClienteEntity>): ClientesListModel {
        val list = ClientesListModel(items.map { cliente(it) })
        list.add(linkTo(CadastroController::class.java).withSelfRel())
        return list
    }

    fun toView(entity: SolicitacaoEntity) =
        SolicitacaoView(
            cpf = entity.cpf,
            nome = entity.nome,
            email = entity.email,
            telefone = entity.telefone,
            salario = entity.salario,
            endereco = toView(entity.endereco),
            status = entity.status.name,
            motivo = entity.motivo,
            dataHoraProcessamento = entity.dataHoraProcessamento,
        )

    fun toView(entity: ClienteEntity) =
        ClienteView(
            cpf = entity.cpf,
            nome = entity.nome,
            email = entity.email,
            telefone = entity.telefone,
            salario = entity.salario,
            endereco = toView(entity.endereco),
        )

    fun toView(endereco: EnderecoEmbeddable) =
        EnderecoView(
            logradouro = endereco.logradouro,
            numero = endereco.numero,
            complemento = endereco.complemento,
            cep = endereco.cep,
            cidade = endereco.cidade,
            uf = endereco.uf,
        )
}
