package br.ufpr.dac.bantads.shared.amqp

object QueueNames {
    const val SAGA_CMD = "saga.cmd"
    const val MS_CLIENTE_CMD = "ms.cliente.cmd"
    const val MS_CONTA_CMD = "ms.conta.cmd"
    const val MS_GERENTE_CMD = "ms.gerente.cmd"
    const val MS_AUTH_CMD = "ms.auth.cmd"
    const val MS_EMAIL_CMD = "ms.email.cmd"
    const val ORQUESTRADOR_REPLY = "orquestrador.reply"
    const val MS_CONTA_EVENTS = "ms.conta.events"

    const val MS_CLIENTE_CMD_WAIT = "ms.cliente.cmd.wait"
    const val MS_CONTA_CMD_WAIT = "ms.conta.cmd.wait"
    const val MS_GERENTE_CMD_WAIT = "ms.gerente.cmd.wait"
    const val MS_AUTH_CMD_WAIT = "ms.auth.cmd.wait"
    const val MS_CONTA_EVENTS_WAIT = "ms.conta.events.wait"

    const val MS_CLIENTE_CMD_DLQ = "ms.cliente.cmd.dlq"
    const val MS_CONTA_CMD_DLQ = "ms.conta.cmd.dlq"
    const val MS_GERENTE_CMD_DLQ = "ms.gerente.cmd.dlq"
    const val MS_AUTH_CMD_DLQ = "ms.auth.cmd.dlq"
    const val MS_CONTA_EVENTS_DLQ = "ms.conta.events.dlq"
}
