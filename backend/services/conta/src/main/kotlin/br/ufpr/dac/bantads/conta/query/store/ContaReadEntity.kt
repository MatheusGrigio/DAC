package br.ufpr.dac.bantads.conta.query.store

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDate

@Entity
@Table(name = "conta")
class ContaReadEntity(
    @Id @Column(length = 4) var numero: String = "",
    @Column(name = "cpf_cliente", nullable = false, length = 11, unique = true) var cpfCliente: String = "",
    @Column(name = "cpf_gerente", nullable = false, length = 11) var cpfGerente: String = "",
    @Column(nullable = false, precision = 19, scale = 4) var saldo: BigDecimal = BigDecimal.ZERO,
    @Column(name = "data_criacao", nullable = false) var dataCriacao: LocalDate = LocalDate.EPOCH,
)
