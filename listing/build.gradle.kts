dependencies {
    implementation(project(":shared"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("io.swagger.core.v3:swagger-annotations-jakarta:2.2.31")
    implementation("com.cloudinary:cloudinary-http44:1.38.0") // Cloudinary SDK
    implementation("com.google.cloud:google-cloud-vertexai:1.1.0") // Gemini API
    implementation("org.springframework.boot:spring-boot-starter-webflux") // For WebClient
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin") // Moved from testImplementation

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
    // testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin") // Removed from here
}

repositories {
    mavenCentral()
}
tasks.test {
    useJUnitPlatform()
}