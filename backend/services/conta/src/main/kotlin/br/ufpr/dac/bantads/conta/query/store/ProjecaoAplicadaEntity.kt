package br.ufpr.dac.bantads.conta.query.store

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "projecao_aplicada")
class ProjecaoAplicadaEntity(
    @Id @Column(name = "evento_id") var eventoId: UUID = UUID.randomUUID(),
    @Column(name = "numero_conta", nullable = false) var numeroConta: String = "",
    @Column(nullable = false) var versao: Int = 0,
)
