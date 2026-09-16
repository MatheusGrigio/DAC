package br.ufpr.dac.bantads.gerente.cadastro

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "gerente")
class GerenteEntity(
    @Id @Column(length = 11) var cpf: String = "",
    @Column(nullable = false) var nome: String = "",
    @Column(nullable = false) var email: String = "",
    @Column(nullable = false) var telefone: String = "",
    @Column(nullable = false) var ativo: Boolean = true,
)
