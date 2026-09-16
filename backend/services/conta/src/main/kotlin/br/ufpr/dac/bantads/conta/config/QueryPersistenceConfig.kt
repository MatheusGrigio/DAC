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
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter
import org.springframework.transaction.PlatformTransactionManager
import javax.sql.DataSource

@Configuration
@EnableConfigurationProperties
@EnableJpaRepositories(
    basePackages = ["br.ufpr.dac.bantads.conta.query"],
    entityManagerFactoryRef = "queryEntityManagerFactory",
    transactionManagerRef = "queryTransactionManager",
)
class QueryPersistenceConfig {
    @Bean
    @ConfigurationProperties("query.datasource")
    fun queryDataSourceProperties(): DataSourceProperties = DataSourceProperties()

    @Bean
    fun queryDataSource(
        @Qualifier("queryDataSourceProperties") properties: DataSourceProperties,
    ): DataSource {
        val dataSource =
            properties
                .initializeDataSourceBuilder()
                .type(HikariDataSource::class.java)
                .build()
        dataSource.poolName = "conta-query"
        dataSource.maximumPoolSize = 4
        return dataSource
    }

    @Bean(initMethod = "migrate")
    fun queryFlyway(
        @Qualifier("queryDataSource") dataSource: DataSource,
    ): Flyway =
        Flyway
            .configure()
            .dataSource(dataSource)
            .schemas("conta_query")
            .locations("classpath:db/query")
            .load()

    @Bean
    @DependsOn("queryFlyway")
    fun queryEntityManagerFactory(
        @Qualifier("queryDataSource") dataSource: DataSource,
    ): LocalContainerEntityManagerFactoryBean {
        val factory = LocalContainerEntityManagerFactoryBean()
        factory.dataSource = dataSource
        factory.setPackagesToScan("br.ufpr.dac.bantads.conta.query")
        factory.persistenceUnitName = "query"
        factory.jpaVendorAdapter = HibernateJpaVendorAdapter()
        factory.setJpaPropertyMap(
            mapOf(
                "hibernate.hbm2ddl.auto" to "validate",
                "hibernate.default_schema" to "conta_query",
                "hibernate.jdbc.time_zone" to "America/Sao_Paulo",
            ),
        )
        return factory
    }

    @Bean
    fun queryTransactionManager(
        @Qualifier("queryEntityManagerFactory") entityManagerFactory: EntityManagerFactory,
    ): PlatformTransactionManager = JpaTransactionManager(entityManagerFactory)
}
