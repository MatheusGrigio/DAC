package br.ufpr.dac.bantads.saga

import br.ufpr.dac.bantads.saga.config.SagaProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(
    scanBasePackages = ["br.ufpr.dac.bantads"],
    exclude = [
        DataSourceAutoConfiguration::class,
        HibernateJpaAutoConfiguration::class,
        FlywayAutoConfiguration::class,
        DataSourceTransactionManagerAutoConfiguration::class,
        JpaRepositoriesAutoConfiguration::class,
    ],
)
@EnableConfigurationProperties(SagaProperties::class)
@EnableScheduling
class SagaApplication

fun main(args: Array<String>) {
    runApplication<SagaApplication>(*args)
}
