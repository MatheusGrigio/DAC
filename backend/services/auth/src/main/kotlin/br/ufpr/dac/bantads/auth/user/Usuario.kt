package br.ufpr.dac.bantads.auth.user

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

@Document("usuarios")
data class Usuario(
    @Id val cpf: String,
    @Indexed(unique = true) val login: String,
    val senhaHash: String,
    val tipo: String,
    val ativo: Boolean,
)
