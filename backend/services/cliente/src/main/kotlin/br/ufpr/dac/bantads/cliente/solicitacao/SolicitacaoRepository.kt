package br.ufpr.dac.bantads.cliente.solicitacao

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface SolicitacaoRepository : JpaRepository<SolicitacaoEntity, String> {
    fun findByCpf(cpf: String): SolicitacaoEntity?

    fun findByEmail(email: String): SolicitacaoEntity?

    @Query(
        value = """
            SELECT * FROM solicitacao
            WHERE (:status = '' OR status = :status)
            ORDER BY nome COLLATE "pt-BR-x-icu"
            """,
        nativeQuery = true,
    )
    fun listByStatus(
        @Param("status") status: String,
    ): List<SolicitacaoEntity>
}
