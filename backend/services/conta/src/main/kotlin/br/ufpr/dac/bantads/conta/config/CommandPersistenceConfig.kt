package br.ufpr.dac.bantads.conta.config

import com.zaxxer.hikari.HikariDataSource
import jakarta.persistence.EntityManagerFactory
import org.flywaydb.core.Flyway
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.DependsOn
import org.springframework.context.annotation.Primary
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter
import org.springframework.transaction.PlatformTransactionManager
import javax.sql.DataSource

@Configuration
@EnableConfigurationProperties
@EnableJpaRepositories(
    basePackages = [
        "br.ufpr.dac.bantads.conta.command",
        "br.ufpr.dac.bantads.conta.saga",
    ],
    entityManagerFactoryRef = "commandEntityManagerFactory",
    transactionManagerRef = "commandTransactionManager",
)
class CommandPersistenceConfig {
    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    fun commandDataSourceProperties(): DataSourceProperties = DataSourceProperties()

    @Bean
    @Primary
    fun dataSource(
        @Qualifier("commandDataSourceProperties") properties: DataSourceProperties,
    ): DataSource {
        val dataSource =
            properties
                .initializeDataSourceBuilder()
                .type(HikariDataSource::class.java)
                .build()
        dataSource.poolName = "conta-command"
        dataSource.maximumPoolSize = 4
        return dataSource
    }

    @Bean(initMethod = "migrate")
    fun commandFlyway(
        @Qualifier("dataSource") dataSource: DataSource,
    ): Flyway =
        Flyway
            .configure()
            .dataSource(dataSource)
            .schemas("conta_command")
            .locations("classpath:db/migration")
            .load()

    @Bean
    @Primary
    @DependsOn("commandFlyway")
    fun commandEntityManagerFactory(
        @Qualifier("dataSource") dataSource: DataSource,
    ): LocalContainerEntityManagerFactoryBean {
        val factory = LocalContainerEntityManagerFactoryBean()
        factory.dataSource = dataSource
        factory.setPackagesToScan(
            "br.ufpr.dac.bantads.conta.command",
            "br.ufpr.dac.bantads.conta.saga",
        )
        factory.persistenceUnitName = "command"
        factory.jpaVendorAdapter = HibernateJpaVendorAdapter()
        factory.setJpaPropertyMap(
            mapOf(
                "hibernate.hbm2ddl.auto" to "validate",
                "hibernate.default_schema" to "conta_command",
                "hibernate.jdbc.time_zone" to "America/Sao_Paulo",
            ),
        )
        return factory
    }

    @Bean(name = ["transactionManager", "commandTransactionManager"])
    @Primary
    fun commandTransactionManager(
        @Qualifier("commandEntityManagerFactory") entityManagerFactory: EntityManagerFactory,
    ): PlatformTransactionManager = JpaTransactionManager(entityManagerFactory)
}
