package br.ufpr.dac.bantads.auth.user

import org.springframework.data.mongodb.repository.MongoRepository

interface UsuarioRepository : MongoRepository<Usuario, String> {
    fun findByLogin(login: String): Usuario?

    fun findByCpf(cpf: String): Usuario?
}
