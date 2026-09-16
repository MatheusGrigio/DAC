package br.ufpr.dac.bantads.gerente.cadastro

import br.ufpr.dac.bantads.gerente.dto.GerenteUpdate
import br.ufpr.dac.bantads.gerente.seed.SeedGerentes
import br.ufpr.dac.bantads.gerente.web.ApiException
import br.ufpr.dac.bantads.shared.error.ErroBody
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GerenteService(
    private val gerentes: GerenteRepository,
) {
    @Transactional(readOnly = true)
    fun listarAtivos(): List<GerenteEntity> = gerentes.listAtivosOrdered()

    @Transactional(readOnly = true)
    fun obter(cpf: String): GerenteEntity = gerentes.findByCpf(cpf) ?: throw ApiException(ErroBody.notFound("Gerente não encontrado"))

    @Transactional
    fun atualizar(
        cpf: String,
        update: GerenteUpdate,
    ): GerenteEntity {
        val atual = obter(cpf)
        val cpfBody = update.cpf?.trim()?.takeIf { it.isNotEmpty() }
        val emailBody =
            update.email
                ?.trim()
                ?.lowercase()
                ?.takeIf { it.isNotEmpty() }
        if (cpfBody != null && cpfBody != atual.cpf) {
            throw ApiException(ErroBody.badRequest("CPF e e-mail são imutáveis"))
        }
        if (emailBody != null && emailBody != atual.email) {
            throw ApiException(ErroBody.badRequest("CPF e e-mail são imutáveis"))
        }
        atual.nome = update.nome.trim()
        atual.telefone = update.telefone.trim()
        return gerentes.save(atual)
    }

    @Transactional
    fun inserir(
        cpf: String,
        nome: String,
        email: String,
        telefone: String,
    ): GerenteEntity {
        val login = email.trim().lowercase()
        if (gerentes.findByCpf(cpf) != null) {
            throw IllegalStateException(GerenteRules.CPF_DUPLICADO)
        }
        if (gerentes.findByEmail(login) != null) {
            throw IllegalStateException(GerenteRules.EMAIL_DUPLICADO)
        }
        return gerentes.save(
            GerenteEntity(
                cpf = cpf.trim(),
                nome = nome.trim(),
                email = login,
                telefone = telefone.trim(),
                ativo = true,
            ),
        )
    }

    @Transactional
    fun remover(cpf: String) {
        gerentes.findByCpf(cpf)?.let { gerentes.delete(it) }
    }

    @Transactional
    fun inativar(cpf: String): GerenteEntity {
        val atual = gerentes.findByCpf(cpf) ?: throw IllegalStateException("Gerente não encontrado")
        if (!GerenteRules.canInativar(atual.ativo, gerentes.countByAtivoTrue())) {
            if (!atual.ativo) throw IllegalStateException("Gerente não encontrado")
            throw IllegalStateException(GerenteRules.ULTIMO_ATIVO)
        }
        atual.ativo = false
        return gerentes.save(atual)
    }

    @Transactional
    fun reativar(cpf: String): GerenteEntity {
        val atual = gerentes.findByCpf(cpf) ?: throw IllegalStateException("Gerente não encontrado")
        atual.ativo = true
        return gerentes.save(atual)
    }

    @Transactional
    fun reboot(): Int {
        gerentes.deleteAll()
        SeedGerentes.ALL.forEach { seed ->
            gerentes.save(
                GerenteEntity(
                    cpf = seed.cpf,
                    nome = seed.nome,
                    email = seed.email,
                    telefone = seed.telefone,
                    ativo = true,
                ),
            )
        }
        return SeedGerentes.ALL.size
    }
}
