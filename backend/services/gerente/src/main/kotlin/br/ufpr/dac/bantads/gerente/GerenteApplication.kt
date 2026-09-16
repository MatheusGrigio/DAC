package br.ufpr.dac.bantads.gerente

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["br.ufpr.dac.bantads"])
class GerenteApplication

fun main(args: Array<String>) {
    runApplication<GerenteApplication>(*args)
}
