import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    kotlin("jvm") version "2.1.10" apply false
    kotlin("plugin.spring") version "2.1.10" apply false
    kotlin("plugin.jpa") version "2.1.10" apply false
    id("org.springframework.boot") version "3.4.5" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0" apply false
}
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
    extra["testcontainers.version"] = "1.21.4"

    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    tasks.named<Copy>("processTestResources") {
        from(rootProject.file("gradle/docker-java.properties"))
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        // Docker Engine 29 rejects docker-java's default API 1.32 (minimum is 1.44).
        systemProperty("api.version", "1.44")
        if (System.getProperty("os.name").lowercase().contains("windows")) {
            environment("DOCKER_HOST", "npipe:////./pipe/dockerDesktopLinuxEngine")
            environment(
                "TESTCONTAINERS_DOCKER_CLIENT_STRATEGY",
                "org.testcontainers.dockerclient.EnvironmentAndSystemPropertyClientProviderStrategy",
            )
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
            add("implementation", "com.fasterxml.jackson.module:jackson-module-kotlin")
            add("implementation", "com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
            add("testImplementation", "org.jetbrains.kotlin:kotlin-test")
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
            add("testImplementation", "org.springframework.boot:spring-boot-starter-test")
        }

        tasks.named<BootJar>("bootJar") {
            archiveFileName.set("app.jar")
        }
    }
}

allprojects {
    group = "br.ufpr.dac.bantads"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    extra["testcontainers.version"] = "1.21.4"

    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    tasks.named<Copy>("processTestResources") {
        from(rootProject.file("gradle/docker-java.properties"))
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        // Docker Engine 29 rejects docker-java's default API 1.32 (minimum is 1.44).
        systemProperty("api.version", "1.44")
        if (System.getProperty("os.name").lowercase().contains("windows")) {
            environment("DOCKER_HOST", "npipe:////./pipe/dockerDesktopLinuxEngine")
            environment(
                "TESTCONTAINERS_DOCKER_CLIENT_STRATEGY",
                "org.testcontainers.dockerclient.EnvironmentAndSystemPropertyClientProviderStrategy",
            )
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
            add("implementation", "com.fasterxml.jackson.module:jackson-module-kotlin")
            add("implementation", "com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
            add("testImplementation", "org.jetbrains.kotlin:kotlin-test")
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
            add("testImplementation", "org.springframework.boot:spring-boot-starter-test")
        }

        tasks.named<BootJar>("bootJar") {
            archiveFileName.set("app.jar")
        }
    }
}
