package br.ufpr.dac.bantads.cliente.domain

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
class EnderecoEmbeddable(
    @Column(nullable = false) var logradouro: String = "",
    @Column(nullable = false) var numero: String = "",
    var complemento: String? = null,
    @Column(nullable = false, length = 8) var cep: String = "",
    @Column(nullable = false) var cidade: String = "",
    @Column(nullable = false, length = 2) var uf: String = "",
)
