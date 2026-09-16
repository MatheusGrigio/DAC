package br.ufpr.dac.bantads.conta.reboot

import br.ufpr.dac.bantads.conta.command.seed.SeedContas
import br.ufpr.dac.bantads.conta.command.store.EventStore
import br.ufpr.dac.bantads.conta.query.project.EventProjector
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class RebootResponse(
    val status: String = "ok",
    val contas: Int,
    val eventos: Int,
)

@Service
class CommandRebootService(
    private val store: EventStore,
) {
    @PersistenceContext(unitName = "command")
    private lateinit var entityManager: EntityManager

    @Transactional
    fun resetAndSeed() =
        run {
            entityManager
                .createNativeQuery("TRUNCATE TABLE conta_command.saga_inbox, conta_command.evento RESTART IDENTITY")
                .executeUpdate()
            entityManager.flush()
            entityManager.clear()
            val seeded = SeedContas.toStored()
            seeded.forEach { store.append(it) }
            seeded
        }
}

@Service
class RebootService(
    private val commandReboot: CommandRebootService,
    private val projector: EventProjector,
) {
    fun reboot(): RebootResponse {
        val seeded = commandReboot.resetAndSeed()
        projector.rebuild(seeded)
        val contas = seeded.map { it.objetoId }.distinct().size
        return RebootResponse(contas = contas, eventos = seeded.size)
    }
}
