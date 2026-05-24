plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.spring") version "2.0.21"
    id("org.springframework.boot") version "3.4.4"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.jetbrains.kotlinx.kover") version "0.9.1"
    kotlin("plugin.jpa") version "2.0.21"
    // Apply Sonar plugin at the root level
    id("org.sonarqube") version "7.3.0.8198"
}

val springBootVersion = "3.4.4"

allprojects {
    group = "com.fanyiadrien"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.plugin.spring")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "org.jetbrains.kotlinx.kover")
    // CRITICAL: Every submodule must apply the sonarqube plugin to be indexed!
    apply(plugin = "org.sonarqube")

    dependencyManagement {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:$springBootVersion")
        }
    }

    configurations.all {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(group = "junit", module = "junit")
    }

    dependencies {
        implementation("org.jetbrains.kotlin:kotlin-reflect")
        implementation("org.springframework.boot:spring-boot-starter")
        implementation("org.springframework.modulith:spring-modulith-starter-core:1.3.0")
        compileOnly("io.swagger.core.v3:swagger-annotations-jakarta:2.2.31")

        implementation("org.springframework.boot:spring-boot-starter-data-redis")

        testImplementation("org.springframework.boot:spring-boot-starter-test")
        testImplementation("org.springframework.modulith:spring-modulith-starter-test:1.3.0")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    }

    kotlin {
        jvmToolchain(21)
    }

    // Configure the subprojects' scanner settings dynamically
    sonarqube {
        properties {
            property("sonar.sources", "src/main/kotlin")
            property("sonar.tests", "src/test/kotlin")
        }
    }
}

// Global cross-project report configuration for Kover 0.9+
kover {
    reports {
        filters {
            excludes {
                classes(
                    "*.dto.*",
                    "*.config.*",
                    "*.IctuExBackendApplicationKt",
                    "com.fanyiadrien.notification.*",
                    "com.fanyiadrien.messaging.*",
                    "com.fanyiadrien.listing.*",
                    "com.fanyiadrien.shared.*",
                    "com.fanyiadrien.sync.*"
                )
            }
        }
        verify {
            rule {
                minBound(90)
            }
        }
        total {
            xml {
                onCheck = true
                // Explicitly define where the global merged report file lands
                xmlFile = file("${rootProject.layout.buildDirectory.get().asFile}/reports/kover/report.xml")
            }
        }
    }
}

// Global Configuration pointing to the unified Kover file
sonarqube {
    properties {
        property("sonar.projectKey", "ictu-ex-backend")
        property("sonar.organization", "fanyicharllson")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.projectName", "ICTU-Ex Smart Student Marketplace")
        property("sonar.projectVersion", "1.0.0")
        property("sonar.sourceEncoding", "UTF-8")

        // Point to the exact root location where Kover outputs the total merged file
        property("sonar.coverage.jacoco.xmlReportPaths", "${rootProject.layout.buildDirectory.get().asFile}/reports/kover/report.xml")

        property("sonar.exclusions", "**/generated/**,**/build/**,**/*Application.kt")
        property("sonar.cpd.exclusions", "**/dto/**,**/model/**")
    }
}

dependencies {
    implementation(kotlin("stdlib"))
}
