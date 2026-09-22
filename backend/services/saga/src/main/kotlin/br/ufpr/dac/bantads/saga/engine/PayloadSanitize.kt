package br.ufpr.dac.bantads.saga.engine

fun Map<String, Any?>.withoutPassword(): Map<String, Any?> =
    filterKeys { key -> key.lowercase() != "senha" && key.lowercase() != "password" }
