package br.ufpr.dac.bantads.email.compose

import br.ufpr.dac.bantads.email.mail.Destinatario

object Destinatarios {
    fun from(payload: Map<String, Any?>): List<Destinatario> {
        val lista = payload["destinatarios"] ?: payload["para"] ?: payload["emails"]
        if (lista is Collection<*>) {
            return lista.mapNotNull { item ->
                when (item) {
                    is Map<*, *> -> {
                        val email = texto(item["email"] ?: item["to"]) ?: return@mapNotNull null
                        Destinatario(email, texto(item["nome"]))
                    }
                    else -> texto(item)?.let { Destinatario(it, null) }
                }
            }
        }
        val email = texto(payload["email"]) ?: texto(payload["to"]) ?: return emptyList()
        return listOf(Destinatario(email, texto(payload["nome"])))
    }

    fun texto(value: Any?): String? = value?.toString()?.trim()?.takeIf { it.isNotEmpty() && it != "null" }
}
