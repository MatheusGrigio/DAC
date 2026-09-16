package br.ufpr.dac.bantads.conta.command.http

import br.ufpr.dac.bantads.conta.command.event.AccountState
import br.ufpr.dac.bantads.conta.command.event.EventTypes
import br.ufpr.dac.bantads.conta.command.event.StoredEvent
import br.ufpr.dac.bantads.conta.command.numbering.AccountNumberGenerator
import br.ufpr.dac.bantads.conta.command.publish.ContaEventPublisher
import br.ufpr.dac.bantads.conta.command.r13.R13Selecao
import br.ufpr.dac.bantads.conta.command.store.EventStore
import br.ufpr.dac.bantads.conta.command.store.toStored
import br.ufpr.dac.bantads.conta.web.ApiException
import br.ufpr.dac.bantads.conta.web.Identity
import br.ufpr.dac.bantads.shared.error.ErroBody
import br.ufpr.dac.bantads.shared.money.Money
import br.ufpr.dac.bantads.shared.time.DateTimes
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.transaction.support.TransactionTemplate
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class ContaCommandService(
    private val store: EventStore,
    private val publisher: ContaEventPublisher,
    transactionManager: PlatformTransactionManager,
) {
    private val tx = TransactionTemplate(transactionManager)

    fun depositar(
        numero: String,
        valor: BigDecimal,
        userCpf: String,
        userTipo: String,
    ): OperacaoRealizadaView = writeMoney(numero, valor, userCpf, userTipo, EventTypes.DEPOSITO, "DEPOSITO")

    fun sacar(
        numero: String,
        valor: BigDecimal,
        userCpf: String,
        userTipo: String,
    ): OperacaoRealizadaView = writeMoney(numero, valor, userCpf, userTipo, EventTypes.SAQUE, "SAQUE")

    fun transferir(
        numero: String,
        body: TransferenciaCommandInput,
        userCpf: String,
        userTipo: String,
    ): OperacaoRealizadaView {
        requirePositive(body.valor)
        if (body.origem.numeroConta != numero) {
            throw ApiException(ErroBody.badRequest("Origem não confere com a conta do path"))
        }
        if (body.destino.numeroConta == numero) {
            throw ApiException(ErroBody.unprocessable("Não é permitido transferir para a própria conta"))
        }
        val parteDestino =
            ParteTransferenciaView(body.destino.numeroConta, body.destino.cpf, body.destino.nome)
        val origemMap = parte(body.origem)
        val destinoMap = parte(body.destino)
        val payload =
            mapOf("valor" to Money.format(body.valor), "origem" to origemMap, "destino" to destinoMap)
        return retry {
            tx.execute {
                val origem = requireExisting(numero)
                Identity.requireClienteOwner(userTipo, userCpf, origem.cpfCliente)
                val destino = store.state(body.destino.numeroConta)
                if (!destino.existe) {
                    throw ApiException(ErroBody.unprocessable("Conta destino inexistente"))
                }
                if (!Money.gte(origem.saldo, body.valor)) {
                    throw ApiException(ErroBody.unprocessable("Saldo insuficiente para a operação"))
                }
                val agora = DateTimes.parse(DateTimes.now())
                val evOrigem =
                    store.append(
                        store.newEvent(
                            numero,
                            EventTypes.TRANSFERENCIA_ORIGEM,
                            payload,
                            origem.versao + 1,
                            agora,
                        ),
                    )
                val evDestino =
                    store.append(
                        store.newEvent(
                            body.destino.numeroConta,
                            EventTypes.TRANSFERENCIA_DESTINO,
                            payload,
                            destino.versao + 1,
                            agora,
                        ),
                    )
                publishAfterCommit(listOf(evOrigem.toStored(), evDestino.toStored()))
                operacao(numero, "TRANSFERENCIA", agora, body.valor, parteDestino)
            }!!
        }
    }

    fun criar(
        cpfCliente: String,
        cpfGerente: String,
        dataCriacao: String = LocalDate.now(DateTimes.ZONE).toString(),
    ): String =
        retry {
            tx.execute {
                val numero = AccountNumberGenerator.generate(cpfCliente, store::exists)
                val event =
                    store.append(
                        store.newEvent(
                            numero,
                            EventTypes.CRIADO,
                            mapOf(
                                "cpfCliente" to cpfCliente,
                                "cpfGerente" to cpfGerente,
                                "saldoInicial" to "0.00",
                                "dataCriacao" to dataCriacao,
                            ),
                            1,
                            DateTimes.parse(DateTimes.now()),
                        ),
                    )
                publishAfterCommit(listOf(event.toStored()))
                numero
            }!!
        }

    /**
     * Compensação de `conta.criar`: o enunciado não tem evento público de remoção.
     * Premissa: apaga o stream só se a versão máxima ainda é o `Criado` (versao ≤ 1).
     * Assim não se remove conta que já movimentou; `Removido` existe no replay só por segurança.
     */
    fun removerSeSoCriado(numero: String) {
        tx.execute {
            val state = store.state(numero)
            if (state.versao <= 1) {
                val tombstone =
                    store.newEvent(
                        numero,
                        EventTypes.REMOVIDO,
                        emptyMap(),
                        state.versao + 1,
                        DateTimes.parse(DateTimes.now()),
                    )
                store.deleteStream(numero)
                publishAfterCommit(listOf(tombstone))
            }
            null
        }
    }

    fun atribuirGerente(
        numero: String,
        cpfGerente: String,
    ) {
        retry {
            tx.execute {
                val state = requireExisting(numero)
                val event =
                    store.append(
                        store.newEvent(
                            numero,
                            EventTypes.GERENTE_ALTERADO,
                            mapOf(
                                "cpfGerente" to cpfGerente,
                                "cpfGerenteAnterior" to state.cpfGerente,
                            ),
                            state.versao + 1,
                            DateTimes.parse(DateTimes.now()),
                        ),
                    )
                publishAfterCommit(listOf(event.toStored()))
                null
            }
        }
    }

    fun escolherGerenteMenosClientes(ativos: List<String>): String? {
        if (ativos.isEmpty()) return null
        val counts = countByGerente()
        return ativos.minBy { counts[it] ?: 0 }
    }

    fun identificarR13(ativos: Set<String>) = R13Selecao.escolher(store.allStates(), ativos)

    fun transferirContasDoGerente(
        cpfRemovido: String,
        ativos: List<String>,
    ): Map<String, Any?> {
        val destino = escolherGerenteMenosClientes(ativos.filter { it != cpfRemovido })
        val contas = store.allStates().filter { it.existe && it.cpfGerente == cpfRemovido }
        if (destino != null) {
            contas.forEach { atribuirGerente(it.numero, destino) }
        }
        return mapOf(
            "clientes" to contas.mapNotNull { it.cpfCliente },
            "contas" to contas.map { mapOf("numero" to it.numero, "cpfGerente" to cpfRemovido) },
            "cpfGerenteDestino" to destino,
            "quantidadeContas" to contas.size,
            "semContas" to contas.isEmpty(),
        )
    }

    fun countByGerente(): Map<String, Int> =
        store
            .allStates()
            .filter { it.existe && it.cpfGerente != null }
            .groupingBy { it.cpfGerente!! }
            .eachCount()

    private fun writeMoney(
        numero: String,
        valor: BigDecimal,
        userCpf: String,
        userTipo: String,
        tipoEvento: String,
        tipoHttp: String,
    ): OperacaoRealizadaView {
        requirePositive(valor)
        return retry {
            tx.execute {
                val state = requireExisting(numero)
                Identity.requireClienteOwner(userTipo, userCpf, state.cpfCliente)
                if (tipoEvento == EventTypes.SAQUE && !Money.gte(state.saldo, valor)) {
                    throw ApiException(ErroBody.unprocessable("Saldo insuficiente para a operação"))
                }
                val agora = DateTimes.parse(DateTimes.now())
                val event =
                    store.append(
                        store.newEvent(
                            numero,
                            tipoEvento,
                            mapOf("valor" to Money.format(valor)),
                            state.versao + 1,
                            agora,
                        ),
                    )
                publishAfterCommit(listOf(event.toStored()))
                operacao(numero, tipoHttp, agora, valor, null)
            }!!
        }
    }

    private fun requireExisting(numero: String): AccountState {
        val state = store.state(numero)
        if (!state.existe) throw ApiException(ErroBody.notFound("Conta não encontrada"))
        return state
    }

    private fun requirePositive(valor: BigDecimal) {
        if (!Money.isPositive(valor)) {
            throw ApiException(ErroBody.unprocessable("Valor deve ser maior que zero"))
        }
    }

    private fun parte(p: ParteTransferenciaInput) = mapOf("numeroConta" to p.numeroConta, "cpf" to p.cpf, "nome" to p.nome)

    private fun operacao(
        numero: String,
        tipo: String,
        agora: LocalDateTime,
        valor: BigDecimal,
        destino: ParteTransferenciaView?,
    ) = OperacaoRealizadaView(
        numeroConta = numero,
        tipo = tipo,
        dataHora = DateTimes.format(agora),
        valor = valor,
        destino = destino,
    )

    private fun publishAfterCommit(events: List<StoredEvent>) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            publisher.publish(events)
            return
        }
        TransactionSynchronizationManager.registerSynchronization(
            object : TransactionSynchronization {
                override fun afterCommit() {
                    publisher.publish(events)
                }
            },
        )
    }

    private fun <T> retry(block: () -> T): T {
        var last: DataIntegrityViolationException? = null
        repeat(8) {
            try {
                return block()
            } catch (ex: DataIntegrityViolationException) {
                last = ex
            }
        }
        throw last ?: error("retry")
    }
}
