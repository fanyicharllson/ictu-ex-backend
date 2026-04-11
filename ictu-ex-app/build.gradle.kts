plugins {
    id("org.springframework.boot")
    id("com.google.cloud.tools.jib") version "3.4.4"

}

springBoot {
    mainClass.set("com.fanyiadrien.ictuexbackend.IctuExBackendApplicationKt")
}
dependencies {
    implementation(project(":shared"))
    implementation(project(":auth"))
    implementation(project(":listing"))
    implementation(project(":notification"))
    implementation(project(":messaging"))
//    implementation(project(":sync"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("org.springframework.modulith:spring-modulith-starter-core")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("com.resend:resend-java:3.1.0")
    
    // Explicitly add Jackson modules for Kotlin and Date/Time support
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.modulith:spring-modulith-starter-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("org.springframework.boot:spring-boot-starter-test")

}
tasks.test {
    useJUnitPlatform()
}
