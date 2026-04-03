plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.spring") version "2.2.21"
    id("org.springframework.boot") version "4.0.5"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.jetbrains.kotlinx.kover") version "0.8.3" // Kover: is a JetBrains-maintained Gradle plugin designed to measure code coverage for Kotlin projects
}

val springBootVersion = "4.0.5"

allprojects {
    group = "com.fanyiadrien"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

// Configuration shared by ALL submodules (Auth, Listing, etc.)
subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.plugin.spring")
    apply(plugin = "io.spring.dependency-management")

    dependencyManagement {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:$springBootVersion")
        }
    }

    dependencies {
        implementation("org.jetbrains.kotlin:kotlin-reflect")
        implementation("org.springframework.boot:spring-boot-starter")

        // Spring Modulith Core - enforces the internal/public rules
        implementation("org.springframework.modulith:spring-modulith-starter-core:1.3.0")

        testImplementation("org.springframework.boot:spring-boot-starter-test")
        testImplementation("org.springframework.modulith:spring-modulith-starter-test:1.3.0")
    }

    kotlin {
        compilerOptions {
            freeCompilerArgs.addAll("-Xjsr305=strict")
        }
    }
}

kover {
    reports {
        filters {
            excludes {
                classes(
                    "*.dto.*",
                    "*.config.*",
                    "*.IctuExBackendApplicationKt"
                )
            }
        }
        verify {
            rule {
                minBound(90) // 90% coverage gate
            }
        }
    }
}
