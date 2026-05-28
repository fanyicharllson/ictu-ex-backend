plugins {
    id("org.springframework.boot")
    id("com.google.cloud.tools.jib") version "3.4.4"
}

val jjwtVersion = rootProject.extra["jjwtVersion"] as String
val resendVersion = rootProject.extra["resendVersion"] as String
val springdocVersion = rootProject.extra["springdocVersion"] as String
val restAssuredVersion = rootProject.extra["restAssuredVersion"] as String // Added
val hamcrestVersion = rootProject.extra["hamcrestVersion"] as String // Added

springBoot {
    mainClass.set("com.fanyiadrien.ictuexbackend.IctuExBackendApplication") // Corrected main class name
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":auth"))
    implementation(project(":listing"))
    implementation(project(":notification"))
    implementation(project(":messaging"))
    implementation(project(":sync"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("org.springframework.modulith:spring-modulith-starter-core")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springdocVersion")
    implementation("com.resend:resend-java:$resendVersion")

    // JWT
    implementation("io.jsonwebtoken:jjwt-api:$jjwtVersion")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:$jjwtVersion")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:$jjwtVersion")

    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.modulith:spring-modulith-starter-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    // RestAssured for API testing
    testImplementation("io.rest-assured:rest-assured:$restAssuredVersion")
    testImplementation("io.rest-assured:kotlin-extensions:$restAssuredVersion")
    testImplementation("io.rest-assured:json-path:$restAssuredVersion")
    testImplementation("io.rest-assured:xml-path:$restAssuredVersion")
    testImplementation("org.hamcrest:hamcrest:$hamcrestVersion")
}

tasks.test {
    useJUnitPlatform()
}