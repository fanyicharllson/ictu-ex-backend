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
    //  apply sonarqube plugin here — it causes path mapping issues with aggregated Kover reports.
    // Sonar analysis happens only at root level (see root sonarqube block below).

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


    tasks.withType<Test> {
        useJUnitPlatform()

        minHeapSize = "128m"
        maxHeapSize = "512m"
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

// Root Sonar configuration block (lets Gradle/Sonar discover module sources automatically)
sonarqube {
    properties {
        property("sonar.projectKey", "ictu-ex-backend")
        property("sonar.organization", "fanyicharllson")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.projectName", "ICTU-Ex Smart Student Marketplace")
        property("sonar.projectVersion", "1.0.0")
        property("sonar.sourceEncoding", "UTF-8")


        // Point to the aggregated Kover report from root buildDir
        // Use BOTH properties: sonar.kotlin.coverage.reportPaths (Kotlin primary) and sonar.coverage.jacoco.xmlReportPaths (fallback)
        val koverReportPath = "${rootProject.layout.buildDirectory.get().asFile}/reports/kover/report.xml"
        property("sonar.kotlin.coverage.reportPaths", koverReportPath)
        property("sonar.coverage.jacoco.xmlReportPaths", koverReportPath)

        // Exclude build artifacts, generated code, config, and application entry points
        property("sonar.exclusions", "**/generated/**,**/build/**,**/*Application.kt,**/*ApplicationKt.kt")
        property("sonar.cpd.exclusions", "**/dto/**,**/model/**,**/config/**")
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

// Ensures the report aggregates metrics AFTER all tests finish executing
tasks.named("koverXmlReport") {
    subprojects.forEach { sub ->
        mustRunAfter(sub.tasks.withType<Test>())
        dependsOn(sub.tasks.withType<Test>())
    }
}

// Ensures the scanner cannot transmit data until the final layout file is populated
tasks.named("sonar") {
    dependsOn(tasks.named("koverXmlReport"))
}
