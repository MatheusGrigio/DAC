import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    kotlin("jvm") version "2.1.10" apply false
    kotlin("plugin.spring") version "2.1.10" apply false
    kotlin("plugin.jpa") version "2.1.10" apply false
    id("org.springframework.boot") version "3.4.5" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0" apply false
}

allprojects {
    group = "br.ufpr.dac.bantads"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        version.set("1.5.0")
    }

    if (name == "shared") {
        apply(plugin = "org.jetbrains.kotlin.plugin.spring")
        apply(plugin = "io.spring.dependency-management")

        configure<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension> {
            imports {
                mavenBom("org.springframework.boot:spring-boot-dependencies:3.4.5")
            }
        }

        dependencies {
            add("implementation", "org.springframework.boot:spring-boot-starter-json")
            add("implementation", "org.springframework.amqp:spring-rabbit")
            add("implementation", "com.fasterxml.jackson.module:jackson-module-kotlin")
            add("implementation", "com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
        }
    } else {
        apply(plugin = "org.jetbrains.kotlin.plugin.spring")
        apply(plugin = "org.jetbrains.kotlin.plugin.jpa")
        apply(plugin = "org.springframework.boot")
        apply(plugin = "io.spring.dependency-management")

        dependencies {
            add("implementation", project(":shared"))
            add("implementation", "org.springframework.boot:spring-boot-starter-web")
            add("implementation", "com.fasterxml.jackson.module:jackson-module-kotlin")
            add("implementation", "org.jetbrains.kotlin:kotlin-reflect")
        }

        tasks.named<BootJar>("bootJar") {
            archiveFileName.set("app.jar")
        }
    }
}
