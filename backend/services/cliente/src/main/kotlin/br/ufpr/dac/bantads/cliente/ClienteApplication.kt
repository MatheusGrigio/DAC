package br.ufpr.dac.bantads.cliente

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["br.ufpr.dac.bantads"])
class ClienteApplication

fun main(args: Array<String>) {
    runApplication<ClienteApplication>(*args)
}
