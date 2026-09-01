package br.ufpr.dac.bantads.cliente.cadastro

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ClienteRepository : JpaRepository<ClienteEntity, String> {
    fun findByCpf(cpf: String): ClienteEntity?

    fun findByEmail(email: String): ClienteEntity?

    fun findByCpfIn(cpfs: Collection<String>): List<ClienteEntity>

    @Query(
        value = """
            SELECT * FROM cliente
            WHERE (:busca = '' OR cpf ILIKE CONCAT('%', :busca, '%')
                OR public.unaccent(nome) ILIKE public.unaccent(CONCAT('%', :busca, '%')))
            ORDER BY nome COLLATE "pt-BR-x-icu"
            """,
        nativeQuery = true,
    )
    fun searchByCpfOrNome(
        @Param("busca") busca: String,
    ): List<ClienteEntity>
}
