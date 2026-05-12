package com.fanyiadrien.ictuexbackend.config

import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.security.SecurityScheme
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme as OpenApiSecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    `in` = SecuritySchemeIn.HEADER
)
class OpenApiConfig {

    @Bean
    fun customOpenAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("ICTU-Ex API — Smart Student Marketplace")
                    .description("REST API for ICTU-Ex, a secure student marketplace for ICT University Cameroon. Built with Kotlin, Spring Boot, Kafka and Kubernetes.")
                    .version("1.0.0")
                    .contact(
                        Contact()
                            .name("Fanyi Charllson & Adrien Tello")
                            .email("fanyicharllson@gmail.com")
                    )
                    .license(License().name("MIT"))
            )
            .components(
                Components().addSecuritySchemes(
                    "bearerAuth",
                    OpenApiSecurityScheme()
                        .type(OpenApiSecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                )
            )
            .addSecurityItem(SecurityRequirement().addList("bearerAuth"))
    }
}

