plugins {
    kotlin("plugin.jpa")
}

val mockitoKotlinVersion = rootProject.extra["mockitoKotlinVersion"] as String
val cloudinaryVersion = rootProject.extra["cloudinaryVersion"] as String
val googleVertexAiVersion = rootProject.extra["googleVertexAiVersion"] as String

dependencies {
    implementation(project(":shared"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.cloudinary:cloudinary-http44:$cloudinaryVersion")
    implementation("com.google.cloud:google-cloud-vertexai:$googleVertexAiVersion")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.mockito.kotlin:mockito-kotlin:$mockitoKotlinVersion")
    implementation(kotlin("stdlib"))
}

repositories {
    mavenCentral()
}

tasks.test {
    useJUnitPlatform()
}
