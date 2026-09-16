package br.ufpr.dac.bantads.conta.query.project

import br.ufpr.dac.bantads.conta.command.event.EventReplay
import br.ufpr.dac.bantads.conta.command.event.EventTypes
import br.ufpr.dac.bantads.conta.command.event.StoredEvent
import br.ufpr.dac.bantads.conta.query.extrato.MovimentacaoTipos
import br.ufpr.dac.bantads.conta.query.store.ContaReadEntity
import br.ufpr.dac.bantads.conta.query.store.ContaReadRepository
import br.ufpr.dac.bantads.conta.query.store.MovimentacaoEntity
import br.ufpr.dac.bantads.conta.query.store.MovimentacaoRepository
import br.ufpr.dac.bantads.conta.query.store.ProjecaoAplicadaEntity
import br.ufpr.dac.bantads.conta.query.store.ProjecaoAplicadaRepository
import br.ufpr.dac.bantads.shared.money.Money
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class EventProjector(
    private val contas: ContaReadRepository,
    private val movimentacoes: MovimentacaoRepository,
    private val aplicadas: ProjecaoAplicadaRepository,
) {
    @PersistenceContext(unitName = "query")
    private lateinit var entityManager: EntityManager

    @Transactional("queryTransactionManager")
    fun rebuild(events: List<StoredEvent>) {
        entityManager
            .createNativeQuery(
                "TRUNCATE TABLE conta_query.movimentacao, conta_query.conta, conta_query.projecao_aplicada RESTART IDENTITY CASCADE",
            ).executeUpdate()
        entityManager.flush()
        entityManager.clear()
        events.forEach { applyInternal(it) }
    }

    @Transactional("queryTransactionManager")
    fun apply(event: StoredEvent) {
        applyInternal(event)
    }

    @Transactional("queryTransactionManager")
    fun applyAll(events: List<StoredEvent>) {
        events.forEach { applyInternal(it) }
    }

    private fun applyInternal(event: StoredEvent) {
        if (aplicadas.existsById(event.id)) return
        when (event.tipo) {
            EventTypes.CRIADO -> criar(event)
            EventTypes.DEPOSITO -> dinheiro(event, MovimentacaoTipos.DEPOSITO, credito = true)
            EventTypes.SAQUE -> dinheiro(event, MovimentacaoTipos.SAQUE, credito = false)
            EventTypes.TRANSFERENCIA_ORIGEM -> transferir(event, credito = false)
            EventTypes.TRANSFERENCIA_DESTINO -> transferir(event, credito = true)
            EventTypes.GERENTE_ALTERADO -> alterarGerente(event)
            EventTypes.REMOVIDO -> remover(event)
        }
        aplicadas.save(ProjecaoAplicadaEntity(event.id))
    }

    private fun criar(event: StoredEvent) {
        val payload = event.payload
        contas.save(
            ContaReadEntity(
                numero = event.objetoId,
                cpfCliente = EventReplay.texto(payload, "cpfCliente") ?: error("cpfCliente"),
                cpfGerente = EventReplay.texto(payload, "cpfGerente") ?: error("cpfGerente"),
                saldo = EventReplay.dinheiro(payload, "saldoInicial") ?: Money.parse("0.00"),
                dataCriacao = LocalDate.parse(EventReplay.texto(payload, "dataCriacao") ?: error("dataCriacao")),
            ),
        )
    }

    private fun dinheiro(
        event: StoredEvent,
        tipo: String,
        credito: Boolean,
    ) {
        val conta = requireConta(event.objetoId)
        val valor = EventReplay.valor(event.payload)
        conta.saldo = if (credito) Money.add(conta.saldo, valor) else Money.subtract(conta.saldo, valor)
        movimentacoes.save(
            MovimentacaoEntity(
                id = event.id,
                numeroConta = event.objetoId,
                dataHora = event.timestamp,
                tipo = tipo,
                valor = valor,
            ),
        )
    }

    private fun transferir(
        event: StoredEvent,
        credito: Boolean,
    ) {
        val conta = requireConta(event.objetoId)
        val valor = EventReplay.valor(event.payload)
        val origem = parte(event.payload, "origem")
        val destino = parte(event.payload, "destino")
        conta.saldo = if (credito) Money.add(conta.saldo, valor) else Money.subtract(conta.saldo, valor)
        movimentacoes.save(
            MovimentacaoEntity(
                id = event.id,
                numeroConta = event.objetoId,
                dataHora = event.timestamp,
                tipo = MovimentacaoTipos.TRANSFERENCIA,
                valor = valor,
                origemNumero = origem?.numero,
                origemCpf = origem?.cpf,
                origemNome = origem?.nome,
                destinoNumero = destino?.numero,
                destinoCpf = destino?.cpf,
                destinoNome = destino?.nome,
            ),
        )
    }

    private fun alterarGerente(event: StoredEvent) {
        val conta = requireConta(event.objetoId)
        conta.cpfGerente = EventReplay.texto(event.payload, "cpfGerente") ?: error("cpfGerente")
    }

    private fun remover(event: StoredEvent) {
        contas.deleteById(event.objetoId)
    }

    private fun requireConta(numero: String): ContaReadEntity =
        contas.findById(numero).orElseThrow { IllegalStateException("Conta $numero não projetada") }

    private fun parte(
        payload: Map<String, Any?>,
        key: String,
    ): ParteEvento? {
        val raw = payload[key] as? Map<*, *> ?: return null
        return ParteEvento(
            numero = raw["numeroConta"]?.toString(),
            cpf = raw["cpf"]?.toString(),
            nome = raw["nome"]?.toString(),
        )
    }

    private data class ParteEvento(
        val numero: String?,
        val cpf: String?,
        val nome: String?,
    )
}
