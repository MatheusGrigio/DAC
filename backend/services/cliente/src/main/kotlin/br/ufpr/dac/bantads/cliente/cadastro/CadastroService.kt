package br.ufpr.dac.bantads.cliente.cadastro

import br.ufpr.dac.bantads.cliente.domain.EnderecoEmbeddable
import br.ufpr.dac.bantads.cliente.dto.ClienteNomeView
import br.ufpr.dac.bantads.cliente.seed.SeedClientes
import br.ufpr.dac.bantads.cliente.solicitacao.SolicitacaoEntity
import br.ufpr.dac.bantads.cliente.solicitacao.SolicitacaoRepository
import br.ufpr.dac.bantads.cliente.web.ApiException
import br.ufpr.dac.bantads.shared.error.ErroBody
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CadastroService(
    private val clientes: ClienteRepository,
    private val solicitacoes: SolicitacaoRepository,
) {
    @Transactional(readOnly = true)
    fun obter(cpf: String): ClienteEntity = clientes.findByCpf(cpf) ?: throw ApiException(ErroBody.notFound("Cliente não encontrado"))

    @Transactional(readOnly = true)
    fun buscar(busca: String?): List<ClienteEntity> = clientes.searchByCpfOrNome(busca?.trim().orEmpty())

    @Transactional(readOnly = true)
    fun nomesPorCpfs(cpfs: List<String>): List<ClienteNomeView> {
        if (cpfs.isEmpty()) return emptyList()
        return clientes.findByCpfIn(cpfs).map { ClienteNomeView(it.cpf, it.nome, it.email) }
    }

    @Transactional
    fun criarAPartirDaSolicitacao(cpf: String): ClienteEntity {
        val solicitacao =
            solicitacoes.findByCpf(cpf) ?: throw IllegalStateException("Solicitação não encontrada")
        clientes.findByCpf(cpf)?.let { return it }
        return clientes.save(copyFrom(solicitacao))
    }

    @Transactional
    fun remover(cpf: String) {
        clientes.findByCpf(cpf)?.let { clientes.delete(it) }
    }

    @Transactional
    fun reboot(): Int {
        clientes.deleteAll()
        solicitacoes.deleteAll()
        SeedClientes.ALL.forEach { seed ->
            clientes.save(
                ClienteEntity(
                    cpf = seed.cpf,
                    nome = seed.nome,
                    email = seed.email,
                    telefone = seed.telefone,
                    salario = seed.salario,
                    endereco =
                        EnderecoEmbeddable(
                            logradouro = seed.logradouro,
                            numero = seed.numero,
                            complemento = null,
                            cep = seed.cep,
                            cidade = seed.cidade,
                            uf = seed.uf,
                        ),
                ),
            )
        }
        return SeedClientes.ALL.size
    }

    private fun copyFrom(solicitacao: SolicitacaoEntity) =
        ClienteEntity(
            cpf = solicitacao.cpf,
            nome = solicitacao.nome,
            email = solicitacao.email,
            telefone = solicitacao.telefone,
            salario = solicitacao.salario,
            endereco =
                EnderecoEmbeddable(
                    logradouro = solicitacao.endereco.logradouro,
                    numero = solicitacao.endereco.numero,
                    complemento = solicitacao.endereco.complemento,
                    cep = solicitacao.endereco.cep,
                    cidade = solicitacao.endereco.cidade,
                    uf = solicitacao.endereco.uf,
                ),
        )
}
