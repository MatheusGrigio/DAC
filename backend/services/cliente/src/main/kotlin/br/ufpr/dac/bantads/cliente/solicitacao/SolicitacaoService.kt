package br.ufpr.dac.bantads.cliente.solicitacao

import br.ufpr.dac.bantads.cliente.cadastro.ClienteRepository
import br.ufpr.dac.bantads.cliente.domain.EnderecoEmbeddable
import br.ufpr.dac.bantads.cliente.dto.AutocadastroInput
import br.ufpr.dac.bantads.cliente.email.EmailCommandPublisher
import br.ufpr.dac.bantads.cliente.web.ApiException
import br.ufpr.dac.bantads.shared.error.ErroBody
import br.ufpr.dac.bantads.shared.time.DateTimes
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SolicitacaoService(
    private val solicitacoes: SolicitacaoRepository,
    private val clientes: ClienteRepository,
    private val emailPublisher: EmailCommandPublisher,
) {
    @Transactional
    fun criar(input: AutocadastroInput): SolicitacaoEntity {
        val cpf = input.cpf.trim()
        val email = input.email.trim().lowercase()
        if (solicitacoes.findByCpf(cpf) != null) {
            throw ApiException(ErroBody.conflict("CPF já possui solicitação"))
        }
        if (clientes.findByCpf(cpf) != null) {
            throw ApiException(ErroBody.conflict("CPF já possui cadastro de cliente"))
        }
        if (solicitacoes.findByEmail(email) != null || clientes.findByEmail(email) != null) {
            throw ApiException(ErroBody.conflict("E-mail já usado em outra solicitação"))
        }
        return solicitacoes.save(
            SolicitacaoEntity(
                cpf = cpf,
                nome = input.nome.trim(),
                email = email,
                telefone = input.telefone.trim(),
                salario = input.salario,
                endereco =
                    EnderecoEmbeddable(
                        logradouro = input.endereco.logradouro.trim(),
                        numero = input.endereco.numero.trim(),
                        complemento =
                            input.endereco.complemento
                                ?.trim()
                                ?.ifBlank { null },
                        cep = input.endereco.cep.trim(),
                        cidade = input.endereco.cidade.trim(),
                        uf =
                            input.endereco.uf
                                .trim()
                                .uppercase(),
                    ),
                status = StatusSolicitacao.PENDENTE,
            ),
        )
    }

    @Transactional(readOnly = true)
    fun listar(status: StatusSolicitacao?): List<SolicitacaoEntity> = solicitacoes.listByStatus(status?.name.orEmpty())

    @Transactional(readOnly = true)
    fun obter(cpf: String): SolicitacaoEntity =
        solicitacoes.findByCpf(cpf) ?: throw ApiException(ErroBody.notFound("Solicitação não encontrada"))

    @Transactional
    fun rejeitar(
        cpf: String,
        motivo: String,
    ): SolicitacaoEntity {
        val solicitacao = obter(cpf)
        if (!SolicitacaoRules.canProcess(solicitacao.status)) {
            throw ApiException(ErroBody.conflict("Solicitação não está PENDENTE"))
        }
        solicitacao.status = StatusSolicitacao.NAO_APROVADA
        solicitacao.motivo = motivo.trim()
        solicitacao.dataHoraProcessamento = DateTimes.parse(DateTimes.now())
        val saved = solicitacoes.save(solicitacao)
        emailPublisher.publishRejeicao(saved.email, saved.nome, saved.motivo.orEmpty())
        return saved
    }

    @Transactional
    fun marcarAprovada(cpf: String): SolicitacaoEntity {
        val solicitacao = solicitacoes.findByCpf(cpf) ?: throw IllegalStateException("Solicitação não encontrada")
        if (!SolicitacaoRules.canProcess(solicitacao.status)) {
            throw IllegalStateException("Solicitação não está PENDENTE")
        }
        solicitacao.status = StatusSolicitacao.APROVADA
        solicitacao.motivo = null
        solicitacao.dataHoraProcessamento = DateTimes.parse(DateTimes.now())
        return solicitacoes.save(solicitacao)
    }

    @Transactional
    fun desmarcarAprovada(cpf: String): SolicitacaoEntity {
        val solicitacao = solicitacoes.findByCpf(cpf) ?: throw IllegalStateException("Solicitação não encontrada")
        if (solicitacao.status == StatusSolicitacao.APROVADA) {
            solicitacao.status = StatusSolicitacao.PENDENTE
            solicitacao.motivo = null
            solicitacao.dataHoraProcessamento = null
            return solicitacoes.save(solicitacao)
        }
        return solicitacao
    }

    @Transactional
    fun marcarNaoAprovada(cpf: String): SolicitacaoEntity {
        val solicitacao = solicitacoes.findByCpf(cpf) ?: throw IllegalStateException("Solicitação não encontrada")
        solicitacao.status = StatusSolicitacao.NAO_APROVADA
        solicitacao.motivo = SolicitacaoRules.EMAIL_DUPLICADO
        solicitacao.dataHoraProcessamento = DateTimes.parse(DateTimes.now())
        return solicitacoes.save(solicitacao)
    }
}
