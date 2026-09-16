package br.ufpr.dac.bantads.gerente.cadastro

object GerenteRules {
    const val ULTIMO_ATIVO = "Não é permitido remover o último gerente ativo"
    const val EMAIL_DUPLICADO = "E-mail já cadastrado"
    const val CPF_DUPLICADO = "CPF já cadastrado"

    fun canInativar(
        targetAtivo: Boolean,
        ativos: Long,
    ): Boolean = targetAtivo && ativos > 1L
}
