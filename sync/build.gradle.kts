plugins {
    id("java")
    id("org.springframework.boot") version "3.4.4"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.spring") version "2.0.21"
    kotlin("plugin.jpa") version "2.0.21"
}

group = "com.fanyiadrien"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":shared"))
    // Core dependencies from root subprojects
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.modulith:spring-modulith-starter-core:1.3.0")
    compileOnly("io.swagger.core.v3:swagger-annotations-jakarta:2.2.31")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // Sync module specific dependencies
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // Test dependencies
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.modulith:spring-modulith-starter-test:1.3.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
