package com.fanyiadrien.ictuexbackend.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        return http
            .csrf { csrf -> csrf.disable() }
            .cors { cors ->
                val source = UrlBasedCorsConfigurationSource()
                val config = CorsConfiguration()
                config.allowCredentials = true
                config.addAllowedOriginPattern("*")
                config.addAllowedHeader("*")
                config.addAllowedMethod("*")
                source.registerCorsConfiguration("/**", config)
                cors.configurationSource(source)
            }
            .sessionManagement { session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .authorizeHttpRequests { auth ->
                auth.requestMatchers("/api/auth/**").permitAll() // Permit auth endpoints
                auth.requestMatchers("/actuator/**").permitAll() // Permit actuator endpoints
                auth.anyRequest().authenticated() // All other requests require authentication
            }
            .build()
    }
}
