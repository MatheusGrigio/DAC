package br.ufpr.dac.bantads.cliente.solicitacao

object SolicitacaoRules {
    const val EMAIL_DUPLICADO = "E-mail já cadastrado"

    fun canProcess(status: StatusSolicitacao): Boolean = status == StatusSolicitacao.PENDENTE
}
