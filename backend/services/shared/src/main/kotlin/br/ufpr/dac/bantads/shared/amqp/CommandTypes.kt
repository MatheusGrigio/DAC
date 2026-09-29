package br.ufpr.dac.bantads.shared.amqp

object CommandTypes {
    const val APROVAR_CLIENTE = "aprovar-cliente"
    const val INSERIR_GERENTE = "inserir-gerente"
    const val REMOVER_GERENTE = "remover-gerente"
    const val RELATORIO_CLIENTES = "relatorio-clientes"

    const val CLIENTE_MARCAR_APROVADA = "cliente.marcar-aprovada"
    const val CLIENTE_DESMARCAR_APROVADA = "cliente.desmarcar-aprovada"
    const val CLIENTE_MARCAR_NAO_APROVADA = "cliente.marcar-nao-aprovada"
    const val CLIENTE_CRIAR = "cliente.criar"
    const val CLIENTE_REMOVER = "cliente.remover"
    const val CLIENTE_OBTER_POR_CPFS = "cliente.obter-por-cpfs"

    const val GERENTE_INSERIR = "gerente.inserir"
    const val GERENTE_REMOVER = "gerente.remover"
    const val GERENTE_INATIVAR = "gerente.inativar"
    const val GERENTE_REATIVAR = "gerente.reativar"
    const val GERENTE_LISTAR_ATIVOS = "gerente.listar-ativos"

    const val CONTA_ESCOLHER_GERENTE_MENOS_CLIENTES = "conta.escolher-gerente-menos-clientes"
    const val CONTA_CRIAR = "conta.criar"
    const val CONTA_REMOVER = "conta.remover"
    const val CONTA_IDENTIFICAR_CONTA_PARA_NOVO_GERENTE = "conta.identificar-conta-para-novo-gerente"
    const val CONTA_ATRIBUIR_GERENTE = "conta.atribuir-gerente"
    const val CONTA_REATRIBUIR_GERENTE = "conta.reatribuir-gerente"
    const val CONTA_TRANSFERIR_CONTAS_DO_GERENTE = "conta.transferir-contas-do-gerente"
    const val CONTA_REVERTER_TRANSFERENCIA_GERENTES = "conta.reverter-transferencia-gerentes"

    const val AUTH_CRIAR_CLIENTE = "auth.criar-cliente"
    const val AUTH_CRIAR_GERENTE = "auth.criar-gerente"
    const val AUTH_REMOVER = "auth.remover"
    const val AUTH_DESATIVAR = "auth.desativar"
    const val AUTH_REATIVAR = "auth.reativar"

    const val EMAIL_SENHA_CLIENTE = "email.senha-cliente"
    const val EMAIL_FALHA_APROVACAO = "email.falha-aprovacao"
    const val EMAIL_REJEICAO = "email.rejeicao"
    const val EMAIL_TROCA_GERENTE = "email.troca-gerente"

    const val SAGA_INVALIDAR_SESSAO = "saga.invalidar-sessao"

    const val ECHO = "echo"
    const val ECHO_PING = "echo.ping"
    const val ECHO_SETUP = "echo.setup"
    const val ECHO_UNDO = "echo.undo"

    val ALL: Set<String> =
        setOf(
            APROVAR_CLIENTE,
            INSERIR_GERENTE,
            REMOVER_GERENTE,
            RELATORIO_CLIENTES,
            CLIENTE_MARCAR_APROVADA,
            CLIENTE_DESMARCAR_APROVADA,
            CLIENTE_MARCAR_NAO_APROVADA,
            CLIENTE_CRIAR,
            CLIENTE_REMOVER,
            CLIENTE_OBTER_POR_CPFS,
            GERENTE_INSERIR,
            GERENTE_REMOVER,
            GERENTE_INATIVAR,
            GERENTE_REATIVAR,
            GERENTE_LISTAR_ATIVOS,
            CONTA_ESCOLHER_GERENTE_MENOS_CLIENTES,
            CONTA_CRIAR,
            CONTA_REMOVER,
            CONTA_IDENTIFICAR_CONTA_PARA_NOVO_GERENTE,
            CONTA_ATRIBUIR_GERENTE,
            CONTA_REATRIBUIR_GERENTE,
            CONTA_TRANSFERIR_CONTAS_DO_GERENTE,
            CONTA_REVERTER_TRANSFERENCIA_GERENTES,
            AUTH_CRIAR_CLIENTE,
            AUTH_CRIAR_GERENTE,
            AUTH_REMOVER,
            AUTH_DESATIVAR,
            AUTH_REATIVAR,
            EMAIL_SENHA_CLIENTE,
            EMAIL_FALHA_APROVACAO,
            EMAIL_REJEICAO,
            EMAIL_TROCA_GERENTE,
            SAGA_INVALIDAR_SESSAO,
            ECHO,
            ECHO_PING,
            ECHO_SETUP,
            ECHO_UNDO,
        )
}
