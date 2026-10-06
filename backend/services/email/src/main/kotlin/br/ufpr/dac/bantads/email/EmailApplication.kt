package br.ufpr.dac.bantads.email

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["br.ufpr.dac.bantads"])
class EmailApplication

fun main(args: Array<String>) {
    runApplication<EmailApplication>(*args)
}
