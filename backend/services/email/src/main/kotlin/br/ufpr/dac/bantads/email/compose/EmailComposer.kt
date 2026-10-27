package br.ufpr.dac.bantads.email.compose

import br.ufpr.dac.bantads.email.mail.Destinatario
import br.ufpr.dac.bantads.email.mail.OutgoingMail
import br.ufpr.dac.bantads.shared.amqp.CommandTypes

object EmailComposer {
    fun compose(
        tipo: String,
        payload: Map<String, Any?>,
        dest: Destinatario,
    ): OutgoingMail {
        val nome = dest.nome ?: Destinatarios.texto(payload["nome"]) ?: "cliente"
        val senha = Destinatarios.texto(payload["senha"])
        val motivo = Destinatarios.texto(payload["motivo"]) ?: "não informado"
        val gerente =
            Destinatarios.texto(payload["nomeGerente"])
                ?: Destinatarios.texto(payload["gerente"])
                ?: "seu novo gerente"
        return when (tipo) {
            CommandTypes.EMAIL_SENHA_CLIENTE ->
                OutgoingMail(
                    to = dest.email,
                    subject = "BANTADS — sua senha de acesso",
                    body =
                        "Olá $nome,\n\n" +
                            "Sua solicitação de cadastro foi aprovada.\n" +
                            "Senha de acesso: ${senha ?: ""}\n\n" +
                            "BANTADS",
                    senha = senha,
                )
            CommandTypes.EMAIL_FALHA_APROVACAO ->
                OutgoingMail(
                    to = dest.email,
                    subject = "BANTADS — não foi possível concluir o cadastro",
                    body =
                        "Olá $nome,\n\n" +
                            "Não foi possível concluir sua solicitação de cadastro.\n" +
                            "Motivo: $motivo\n\n" +
                            "BANTADS",
                )
            CommandTypes.EMAIL_REJEICAO ->
                OutgoingMail(
                    to = dest.email,
                    subject = "BANTADS — solicitação não aprovada",
                    body =
                        "Olá $nome,\n\n" +
                            "Sua solicitação de autocadastro não foi aprovada.\n" +
                            "Motivo: $motivo\n\n" +
                            "BANTADS",
                )
            CommandTypes.EMAIL_TROCA_GERENTE ->
                OutgoingMail(
                    to = dest.email,
                    subject = "BANTADS — alteração de gerente",
                    body =
                        "Olá $nome,\n\n" +
                            "O gerente responsável pela sua conta foi alterado.\n" +
                            "Novo gerente: $gerente\n\n" +
                            "BANTADS",
                )
            else -> error("tipo de e-mail desconhecido: $tipo")
        }
    }
}
