package br.ufpr.dac.bantads.auth.user

import br.ufpr.dac.bantads.auth.dto.VerificarResponse
import br.ufpr.dac.bantads.auth.password.RandomPassword
import br.ufpr.dac.bantads.auth.seed.SeedUsers
import br.ufpr.dac.bantads.shared.domain.Perfil
import org.springframework.dao.DuplicateKeyException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val usuarios: UsuarioRepository,
    private val passwordEncoder: PasswordEncoder,
) {
    fun verificar(
        email: String,
        senha: String,
    ): VerificarResponse? {
        val user = usuarios.findByLogin(normalizeLogin(email)) ?: return null
        if (!user.ativo) return null
        if (!passwordEncoder.matches(senha, user.senhaHash)) return null
        return VerificarResponse(cpf = user.cpf, tipo = user.tipo)
    }

    fun criarCliente(
        cpf: String,
        email: String,
    ): ResultadoCriacao {
        val login = normalizeLogin(email)
        if (usuarios.findByLogin(login) != null) {
            return ResultadoCriacao.Falha(EMAIL_DUPLICADO)
        }
        if (usuarios.findByCpf(cpf) != null) {
            return ResultadoCriacao.Falha(CPF_DUPLICADO)
        }
        val senha = RandomPassword.generate()
        return persist(cpf, login, senha, Perfil.CLIENTE.wire, senha)
    }

    fun criarGerente(
        cpf: String,
        email: String,
        senha: String,
    ): ResultadoCriacao {
        val login = normalizeLogin(email)
        if (usuarios.findByLogin(login) != null) {
            return ResultadoCriacao.Falha(EMAIL_DUPLICADO)
        }
        if (usuarios.findByCpf(cpf) != null) {
            return ResultadoCriacao.Falha(CPF_DUPLICADO)
        }
        return persist(cpf, login, senha, Perfil.GERENTE.wire, senhaClara = null)
    }

    fun remover(cpf: String) {
        usuarios.deleteById(cpf)
    }

    fun desativar(cpf: String): Boolean = setAtivo(cpf, false)

    fun reativar(cpf: String): Boolean = setAtivo(cpf, true)

    fun reboot(): Int {
        usuarios.deleteAll()
        val senhaHash = passwordEncoder.encode(SeedUsers.SENHA)
        SeedUsers.ALL.forEach { seed ->
            usuarios.save(
                Usuario(
                    cpf = seed.cpf,
                    login = normalizeLogin(seed.login),
                    senhaHash = senhaHash,
                    tipo = seed.tipo,
                    ativo = true,
                ),
            )
        }
        return SeedUsers.ALL.size
    }

    private fun persist(
        cpf: String,
        login: String,
        senha: String,
        tipo: String,
        senhaClara: String?,
    ): ResultadoCriacao {
        val usuario =
            Usuario(
                cpf = cpf,
                login = login,
                senhaHash = passwordEncoder.encode(senha),
                tipo = tipo,
                ativo = true,
            )
        return try {
            usuarios.save(usuario)
            ResultadoCriacao.Sucesso(cpf = cpf, senhaClara = senhaClara)
        } catch (_: DuplicateKeyException) {
            ResultadoCriacao.Falha(EMAIL_DUPLICADO)
        }
    }

    private fun setAtivo(
        cpf: String,
        ativo: Boolean,
    ): Boolean {
        val user = usuarios.findByCpf(cpf) ?: return false
        usuarios.save(user.copy(ativo = ativo))
        return true
    }

    companion object {
        const val EMAIL_DUPLICADO = "E-mail já cadastrado"
        const val CPF_DUPLICADO = "CPF já cadastrado"

        fun normalizeLogin(email: String): String = email.trim().lowercase()
    }
}

sealed class ResultadoCriacao {
    data class Sucesso(
        val cpf: String,
        val senhaClara: String?,
    ) : ResultadoCriacao()

    data class Falha(
        val erro: String,
    ) : ResultadoCriacao()
}
