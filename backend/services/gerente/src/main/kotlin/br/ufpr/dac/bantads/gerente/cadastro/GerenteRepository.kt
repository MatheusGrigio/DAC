package br.ufpr.dac.bantads.gerente.cadastro

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface GerenteRepository : JpaRepository<GerenteEntity, String> {
    fun findByCpf(cpf: String): GerenteEntity?

    fun findByEmail(email: String): GerenteEntity?

    fun countByAtivoTrue(): Long

    @Query(
        value = """
            SELECT * FROM gerente
            WHERE ativo = true
            ORDER BY nome COLLATE "pt-BR-x-icu"
            """,
        nativeQuery = true,
    )
    fun listAtivosOrdered(): List<GerenteEntity>
}
