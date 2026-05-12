dependencies {
    implementation(project(":shared"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("io.swagger.core.v3:swagger-annotations-jakarta:2.2.31")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
    testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin")
}

repositories {
    mavenCentral()
}
tasks.test {
    useJUnitPlatform()
}

