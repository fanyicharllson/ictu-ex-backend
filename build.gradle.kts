import org.gradle.api.tasks.testing.Test

plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.spring") version "2.0.21"
    id("org.springframework.boot") version "3.4.4"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.jetbrains.kotlinx.kover") version "0.9.1"
    kotlin("plugin.jpa") version "2.0.21"
    id("org.sonarqube") version "7.3.0.8198"
}

val springBootVersion = "3.4.4"
val springModulithVersion = "1.3.0"
val swaggerAnnotationsVersion = "2.2.31"
val mockitoKotlinVersion = "5.4.0"
val jjwtVersion = "0.12.6"
val resendVersion = "3.1.0"
val cloudinaryVersion = "1.38.0"
val googleVertexAiVersion = "1.1.0"
val springdocVersion = "2.8.6"

extra["springModulithVersion"] = springModulithVersion
extra["swaggerAnnotationsVersion"] = swaggerAnnotationsVersion
extra["mockitoKotlinVersion"] = mockitoKotlinVersion
extra["jjwtVersion"] = jjwtVersion
extra["resendVersion"] = resendVersion
extra["cloudinaryVersion"] = cloudinaryVersion
extra["googleVertexAiVersion"] = googleVertexAiVersion
extra["springdocVersion"] = springdocVersion

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
    apply(plugin = "org.sonarqube")

    dependencyLocking {
        lockAllConfigurations()
    }

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
        implementation("org.springframework.modulith:spring-modulith-starter-core:$springModulithVersion")
        compileOnly("io.swagger.core.v3:swagger-annotations-jakarta:$swaggerAnnotationsVersion")

        implementation("org.springframework.boot:spring-boot-starter-data-redis")

        testImplementation("org.springframework.boot:spring-boot-starter-test")
        testImplementation("org.springframework.modulith:spring-modulith-starter-test:$springModulithVersion")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    }

    kotlin {
        jvmToolchain(21)
    }

    // ROBUST RELATIVE PATH CONFIGURATION FOR SUBPROJECTS
    sonarqube {
        properties {
            // Use simple relative paths so the Sonar container indexes them cleanly
            property("sonar.sources", "src/main/kotlin")

            val testSrc = file("src/test/kotlin")
            if (testSrc.exists()) {
                property("sonar.tests", "src/test/kotlin")
            } else {
                property("sonar.tests", "")
            }

            // Bind the shared root report path to every module instance
            val centralReportFile = "${rootProject.layout.buildDirectory.get().asFile}/reports/kover/report.xml"
            property("sonar.coverage.jacoco.xmlReportPaths", centralReportFile)
            property("sonar.kotlin.coverage.reportPaths", centralReportFile)
        }
    }
    tasks.withType<Test> {
        useJUnitPlatform()

        // LIMIT FORKED TEST JVM MEMORY
        minHeapSize = "128m"
        maxHeapSize = "512m"

        // Force a fresh JVM process after every 10 tests to dump stale memory contexts
        forkEvery = 10

        testLogging {
            events("started", "passed", "skipped", "failed")
            showStandardStreams = true
        }
        finalizedBy(tasks.named("koverXmlReport"))
    }
}

// Unified multi-project Kover configuration
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
                minBound(80)
            }
        }
        total {
            xml {
                onCheck = true
                xmlFile = file("${rootProject.layout.buildDirectory.get().asFile}/reports/kover/report.xml")
            }
        }
    }
}

// Root Sonar configuration block
sonarqube {
    properties {
        property("sonar.projectKey", "ictu-ex-backend")
        property("sonar.organization", "fanyicharllson")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.projectName", "ICTU-Ex Smart Student Marketplace")
        property("sonar.projectVersion", "1.0.0")
        property("sonar.sourceEncoding", "UTF-8")

        // Target the master Kover XML output file
        val rootReport = "${rootProject.layout.buildDirectory.get().asFile}/reports/kover/report.xml"
        property("sonar.coverage.jacoco.xmlReportPaths", rootReport)
        property("sonar.kotlin.coverage.reportPaths", rootReport)

        property("sonar.exclusions", "**/generated/**,**/build/**,**/*Application.kt")
        property("sonar.cpd.exclusions", "**/dto/**,**/model/**")
    }
}

dependencies {
    implementation(kotlin("stdlib"))
}

project(":ictu-ex-app") {
    afterEvaluate {
        tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
            archiveFileName.set("ictu-ex-app-boot.jar")
        }
    }
}

// Ensure execution synchronization across the task lifecycle graph
tasks.named("koverXmlReport") {
    subprojects.forEach { sub ->
        mustRunAfter(sub.tasks.withType<Test>())
        dependsOn(sub.tasks.withType<Test>())
    }
}

// Force the parent sonar task to explicitly depend on the report compilation step
tasks.named("sonar") {
    dependsOn(tasks.named("koverXmlReport"))
}
