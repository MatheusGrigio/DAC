package br.ufpr.dac.bantads.conta.query.http

import br.ufpr.dac.bantads.conta.query.extrato.ExtratoRegras
import br.ufpr.dac.bantads.conta.query.extrato.MovimentoDelta
import br.ufpr.dac.bantads.conta.query.store.ContaReadEntity
import br.ufpr.dac.bantads.conta.query.store.ContaReadRepository
import br.ufpr.dac.bantads.conta.query.store.MovimentacaoEntity
import br.ufpr.dac.bantads.conta.query.store.MovimentacaoRepository
import br.ufpr.dac.bantads.conta.web.ApiException
import br.ufpr.dac.bantads.conta.web.Identity
import br.ufpr.dac.bantads.shared.error.ErroBody
import br.ufpr.dac.bantads.shared.time.DateTimes
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class ContaQueryService(
    private val contas: ContaReadRepository,
    private val movimentacoes: MovimentacaoRepository,
) {
    @Transactional(transactionManager = "queryTransactionManager", readOnly = true)
    fun obterPorNumero(
        numero: String,
        userCpf: String,
        userTipo: String,
    ): ContaView {
        val conta = contas.findById(numero).orElseThrow { ApiException(ErroBody.notFound("Conta não encontrada")) }
        Identity.requireGerenteOrOwner(userTipo, userCpf, conta.cpfCliente)
        return toView(conta)
    }

    @Transactional(transactionManager = "queryTransactionManager", readOnly = true)
    fun obterPorCpf(
        cpf: String,
        userCpf: String,
        userTipo: String,
    ): ContaView {
        Identity.requireGerenteOrSelf(userTipo, userCpf, cpf)
        val conta = contas.findByCpfCliente(cpf) ?: throw ApiException(ErroBody.notFound("Conta não encontrada"))
        return toView(conta)
    }

    @Transactional(transactionManager = "queryTransactionManager", readOnly = true)
    fun obterInterno(numero: String): InternalContaView {
        val conta = contas.findById(numero).orElseThrow { ApiException(ErroBody.notFound("Conta não encontrada")) }
        return InternalContaView(numero = conta.numero, cpfCliente = conta.cpfCliente)
    }

    @Transactional(transactionManager = "queryTransactionManager", readOnly = true)
    fun extrato(
        numero: String,
        inicio: LocalDate?,
        fim: LocalDate?,
        userCpf: String,
        userTipo: String,
    ): ExtratoView {
        val conta = contas.findById(numero).orElseThrow { ApiException(ErroBody.notFound("Conta não encontrada")) }
        Identity.requireClienteOwner(userTipo, userCpf, conta.cpfCliente)
        val hoje = LocalDate.now(DateTimes.ZONE)
        val (de, ate) = ExtratoRegras.periodo(inicio, fim, hoje)
        val inicioTs = de.atStartOfDay()
        val fimExclusivo = ate.plusDays(1).atStartOfDay()
        val antes =
            movimentacoes.findByNumeroContaAndDataHoraLessThanOrderByDataHoraAsc(numero, inicioTs).map { it.delta() }
        val periodo =
            movimentacoes.findByNumeroContaAndDataHoraGreaterThanEqualAndDataHoraLessThanOrderByDataHoraAsc(
                numero,
                inicioTs,
                fimExclusivo,
            )
        return ExtratoView(
            numeroConta = numero,
            dataInicio = de,
            dataFim = ate,
            saldoAbertura = ExtratoRegras.saldoAbertura(numero, antes),
            movimentacoes = periodo.map { it.toView() },
        )
    }

    @Transactional(transactionManager = "queryTransactionManager", readOnly = true)
    fun saldos(): Map<String, SaldoInternoView> =
        contas.findAll().associate { conta ->
            conta.cpfCliente to SaldoInternoView(conta.saldo, conta.numero, conta.cpfGerente)
        }

    @Transactional(transactionManager = "queryTransactionManager", readOnly = true)
    fun contagemPorGerente(): Map<String, Int> =
        contas
            .findAll()
            .groupingBy { it.cpfGerente }
            .eachCount()

    private fun toView(conta: ContaReadEntity) =
        ContaView(
            numero = conta.numero,
            cpfCliente = conta.cpfCliente,
            cpfGerente = conta.cpfGerente,
            saldo = conta.saldo,
            dataCriacao = conta.dataCriacao,
        )

    private fun MovimentacaoEntity.delta() = MovimentoDelta(tipo, valor, origemNumero, destinoNumero)

    private fun MovimentacaoEntity.toView() =
        MovimentacaoView(
            dataHora = DateTimes.format(dataHora),
            tipo = tipo,
            valor = valor,
            origem = parte(origemNumero, origemCpf, origemNome),
            destino = parte(destinoNumero, destinoCpf, destinoNome),
        )

    private fun parte(
        numero: String?,
        cpf: String?,
        nome: String?,
    ): ParteMovimentacaoView? {
        if (numero.isNullOrBlank() || cpf.isNullOrBlank() || nome.isNullOrBlank()) return null
        return ParteMovimentacaoView(numero, cpf, nome)
    }
}
