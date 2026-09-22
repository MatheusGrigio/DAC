package br.ufpr.dac.bantads.saga.engine

import br.ufpr.dac.bantads.shared.amqp.CommandTypes
import br.ufpr.dac.bantads.shared.amqp.QueueNames
import org.springframework.stereotype.Component

@Component
class SagaRegistry {
    private val definitions: Map<String, SagaDefinition> =
        listOf(echo(), aprovarCliente(), inserirGerente(), removerGerente()).associateBy { it.tipo }

    fun get(tipo: String): SagaDefinition? = definitions[tipo]

    companion object {
        fun echo(): SagaDefinition =
            SagaDefinition(
                tipo = CommandTypes.ECHO,
                steps =
                    listOf(
                        SagaStep(
                            tipo = CommandTypes.ECHO_SETUP,
                            kind = StepKind.LOCAL,
                            compensationTipo = CommandTypes.ECHO_UNDO,
                            compensationKind = StepKind.LOCAL,
                        ),
                        SagaStep(
                            tipo = CommandTypes.ECHO_PING,
                            kind = StepKind.TRANSACTIONAL,
                            queue = QueueNames.MS_AUTH_CMD,
                        ),
                    ),
            )

        fun aprovarCliente(): SagaDefinition =
            SagaDefinition(
                tipo = CommandTypes.APROVAR_CLIENTE,
                steps =
                    listOf(
                        SagaStep(
                            tipo = CommandTypes.CLIENTE_MARCAR_APROVADA,
                            kind = StepKind.TRANSACTIONAL,
                            queue = QueueNames.MS_CLIENTE_CMD,
                            compensationTipo = CommandTypes.CLIENTE_DESMARCAR_APROVADA,
                            compensationKind = StepKind.TRANSACTIONAL,
                            compensationQueue = QueueNames.MS_CLIENTE_CMD,
                        ),
                        SagaStep(
                            tipo = CommandTypes.GERENTE_LISTAR_ATIVOS,
                            kind = StepKind.TRANSACTIONAL,
                            queue = QueueNames.MS_GERENTE_CMD,
                        ),
                        SagaStep(
                            tipo = CommandTypes.CONTA_ESCOLHER_GERENTE_MENOS_CLIENTES,
                            kind = StepKind.TRANSACTIONAL,
                            queue = QueueNames.MS_CONTA_CMD,
                        ),
                        SagaStep(
                            tipo = CommandTypes.CLIENTE_CRIAR,
                            kind = StepKind.TRANSACTIONAL,
                            queue = QueueNames.MS_CLIENTE_CMD,
                            compensationTipo = CommandTypes.CLIENTE_REMOVER,
                            compensationKind = StepKind.TRANSACTIONAL,
                            compensationQueue = QueueNames.MS_CLIENTE_CMD,
                        ),
                        SagaStep(
                            tipo = CommandTypes.AUTH_CRIAR_CLIENTE,
                            kind = StepKind.TRANSACTIONAL,
                            queue = QueueNames.MS_AUTH_CMD,
                            compensationTipo = CommandTypes.AUTH_REMOVER,
                            compensationKind = StepKind.TRANSACTIONAL,
                            compensationQueue = QueueNames.MS_AUTH_CMD,
                        ),
                        SagaStep(
                            tipo = CommandTypes.CONTA_CRIAR,
                            kind = StepKind.TRANSACTIONAL,
                            queue = QueueNames.MS_CONTA_CMD,
                            compensationTipo = CommandTypes.CONTA_REMOVER,
                            compensationKind = StepKind.TRANSACTIONAL,
                            compensationQueue = QueueNames.MS_CONTA_CMD,
                        ),
                        SagaStep(
                            tipo = CommandTypes.EMAIL_SENHA_CLIENTE,
                            kind = StepKind.FIRE_AND_FORGET,
                            queue = QueueNames.MS_EMAIL_CMD,
                        ),
                    ),
            )

        fun inserirGerente(): SagaDefinition =
            SagaDefinition(
                tipo = CommandTypes.INSERIR_GERENTE,
                steps =
                    listOf(
                        SagaStep(
                            tipo = CommandTypes.GERENTE_INSERIR,
                            kind = StepKind.TRANSACTIONAL,
                            queue = QueueNames.MS_GERENTE_CMD,
                            compensationTipo = CommandTypes.GERENTE_REMOVER,
                            compensationKind = StepKind.TRANSACTIONAL,
                            compensationQueue = QueueNames.MS_GERENTE_CMD,
                        ),
                        SagaStep(
                            tipo = CommandTypes.AUTH_CRIAR_GERENTE,
                            kind = StepKind.TRANSACTIONAL,
                            queue = QueueNames.MS_AUTH_CMD,
                            compensationTipo = CommandTypes.AUTH_REMOVER,
                            compensationKind = StepKind.TRANSACTIONAL,
                            compensationQueue = QueueNames.MS_AUTH_CMD,
                        ),
                        SagaStep(
                            tipo = CommandTypes.GERENTE_LISTAR_ATIVOS,
                            kind = StepKind.TRANSACTIONAL,
                            queue = QueueNames.MS_GERENTE_CMD,
                        ),
                        SagaStep(
                            tipo = CommandTypes.CONTA_IDENTIFICAR_CONTA_PARA_NOVO_GERENTE,
                            kind = StepKind.TRANSACTIONAL,
                            queue = QueueNames.MS_CONTA_CMD,
                        ),
                        SagaStep(
                            tipo = CommandTypes.CONTA_ATRIBUIR_GERENTE,
                            kind = StepKind.TRANSACTIONAL,
                            queue = QueueNames.MS_CONTA_CMD,
                            compensationTipo = CommandTypes.CONTA_REATRIBUIR_GERENTE,
                            compensationKind = StepKind.TRANSACTIONAL,
                            compensationQueue = QueueNames.MS_CONTA_CMD,
                            skipIfTrue = "semConta",
                        ),
                        SagaStep(
                            tipo = CommandTypes.CLIENTE_OBTER_POR_CPFS,
                            kind = StepKind.TRANSACTIONAL,
                            queue = QueueNames.MS_CLIENTE_CMD,
                            skipIfTrue = "semConta",
                        ),
                        SagaStep(
                            tipo = CommandTypes.EMAIL_TROCA_GERENTE,
                            kind = StepKind.FIRE_AND_FORGET,
                            queue = QueueNames.MS_EMAIL_CMD,
                            skipIfTrue = "semConta",
                        ),
                    ),
            )

        fun removerGerente(): SagaDefinition =
            SagaDefinition(
                tipo = CommandTypes.REMOVER_GERENTE,
                steps =
                    listOf(
                        SagaStep(
                            tipo = CommandTypes.GERENTE_INATIVAR,
                            kind = StepKind.TRANSACTIONAL,
                            queue = QueueNames.MS_GERENTE_CMD,
                            compensationTipo = CommandTypes.GERENTE_REATIVAR,
                            compensationKind = StepKind.TRANSACTIONAL,
                            compensationQueue = QueueNames.MS_GERENTE_CMD,
                        ),
                        SagaStep(
                            tipo = CommandTypes.AUTH_DESATIVAR,
                            kind = StepKind.TRANSACTIONAL,
                            queue = QueueNames.MS_AUTH_CMD,
                            compensationTipo = CommandTypes.AUTH_REATIVAR,
                            compensationKind = StepKind.TRANSACTIONAL,
                            compensationQueue = QueueNames.MS_AUTH_CMD,
                        ),
                        SagaStep(
                            tipo = CommandTypes.SAGA_INVALIDAR_SESSAO,
                            kind = StepKind.LOCAL,
                        ),
                        SagaStep(
                            tipo = CommandTypes.GERENTE_LISTAR_ATIVOS,
                            kind = StepKind.TRANSACTIONAL,
                            queue = QueueNames.MS_GERENTE_CMD,
                        ),
                        SagaStep(
                            tipo = CommandTypes.CONTA_TRANSFERIR_CONTAS_DO_GERENTE,
                            kind = StepKind.TRANSACTIONAL,
                            queue = QueueNames.MS_CONTA_CMD,
                            compensationTipo = CommandTypes.CONTA_REVERTER_TRANSFERENCIA_GERENTES,
                            compensationKind = StepKind.TRANSACTIONAL,
                            compensationQueue = QueueNames.MS_CONTA_CMD,
                        ),
                        SagaStep(
                            tipo = CommandTypes.CLIENTE_OBTER_POR_CPFS,
                            kind = StepKind.TRANSACTIONAL,
                            queue = QueueNames.MS_CLIENTE_CMD,
                            skipIfTrue = "semContas",
                        ),
                        SagaStep(
                            tipo = CommandTypes.EMAIL_TROCA_GERENTE,
                            kind = StepKind.FIRE_AND_FORGET,
                            queue = QueueNames.MS_EMAIL_CMD,
                            skipIfTrue = "semContas",
                        ),
                    ),
            )
    }
}
