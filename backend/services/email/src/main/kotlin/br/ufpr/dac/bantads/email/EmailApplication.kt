package br.ufpr.dac.bantads.email

import br.ufpr.dac.bantads.email.mail.MailProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

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
@EnableConfigurationProperties(MailProperties::class)
class EmailApplication

fun main(args: Array<String>) {
    runApplication<EmailApplication>(*args)
}
