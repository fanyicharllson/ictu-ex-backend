plugins {
    kotlin("plugin.jpa")
}

val mockitoKotlinVersion = rootProject.extra["mockitoKotlinVersion"] as String

dependencies {
    implementation(project(":shared"))
    implementation(project(":auth"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.mockito.kotlin:mockito-kotlin:$mockitoKotlinVersion")
}

tasks.test {
    useJUnitPlatform()
}
