package br.ufpr.dac.bantads.conta.query.store

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ProjecaoAplicadaRepository : JpaRepository<ProjecaoAplicadaEntity, UUID>
