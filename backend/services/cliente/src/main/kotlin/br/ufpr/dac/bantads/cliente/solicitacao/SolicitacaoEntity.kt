package br.ufpr.dac.bantads.cliente.solicitacao

import br.ufpr.dac.bantads.cliente.domain.EnderecoEmbeddable
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "solicitacao")
class SolicitacaoEntity(
    @Id @Column(length = 11) var cpf: String = "",
    @Column(nullable = false) var nome: String = "",
    @Column(nullable = false) var email: String = "",
    @Column(nullable = false) var telefone: String = "",
    @Column(nullable = false, precision = 19, scale = 4) var salario: BigDecimal = BigDecimal.ZERO,
    @Embedded var endereco: EnderecoEmbeddable = EnderecoEmbeddable(),
    @Enumerated(EnumType.STRING) @Column(nullable = false) var status: StatusSolicitacao = StatusSolicitacao.PENDENTE,
    var motivo: String? = null,
    var dataHoraProcessamento: LocalDateTime? = null,
)
