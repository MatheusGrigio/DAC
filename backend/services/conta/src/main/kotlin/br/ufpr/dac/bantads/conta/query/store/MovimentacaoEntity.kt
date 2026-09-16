package br.ufpr.dac.bantads.conta.query.store

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "movimentacao")
class MovimentacaoEntity(
    @Id var id: UUID = UUID.randomUUID(),
    @Column(name = "numero_conta", nullable = false, length = 4) var numeroConta: String = "",
    @Column(name = "data_hora", nullable = false) var dataHora: LocalDateTime = LocalDateTime.now(),
    @Column(nullable = false, length = 20) var tipo: String = "",
    @Column(nullable = false, precision = 19, scale = 4) var valor: BigDecimal = BigDecimal.ZERO,
    @Column(name = "origem_numero", length = 4) var origemNumero: String? = null,
    @Column(name = "origem_cpf", length = 11) var origemCpf: String? = null,
    @Column(name = "origem_nome", length = 120) var origemNome: String? = null,
    @Column(name = "destino_numero", length = 4) var destinoNumero: String? = null,
    @Column(name = "destino_cpf", length = 11) var destinoCpf: String? = null,
    @Column(name = "destino_nome", length = 120) var destinoNome: String? = null,
)
