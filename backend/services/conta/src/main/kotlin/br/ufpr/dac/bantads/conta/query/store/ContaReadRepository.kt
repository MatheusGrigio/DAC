package br.ufpr.dac.bantads.conta.query.store

import org.springframework.data.jpa.repository.JpaRepository

interface ContaReadRepository : JpaRepository<ContaReadEntity, String> {
    fun findByCpfCliente(cpfCliente: String): ContaReadEntity?
}
