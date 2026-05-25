val mockitoKotlinVersion = rootProject.extra["mockitoKotlinVersion"] as String
val resendVersion = rootProject.extra["resendVersion"] as String

dependencies {
    implementation(project(":shared"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.resend:resend-java:$resendVersion")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.mockito.kotlin:mockito-kotlin:$mockitoKotlinVersion")
}

tasks.test {
    useJUnitPlatform()
}
