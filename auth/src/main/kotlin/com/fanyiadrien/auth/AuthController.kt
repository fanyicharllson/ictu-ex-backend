package com.fanyiadrien.auth

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(private val authService: AuthService) {

    @PostMapping("/register")
    fun register(@RequestBody request: RegisterRequest): ResponseEntity<AuthResult> {
        val result = authService.register(
            email = request.email,
            password = request.password,
            displayName = request.displayName,
            studentId = request.studentId
        )
        return ResponseEntity.ok(result)
    }

    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<AuthResult> {
        val result = authService.login(request.email, request.password)
        return ResponseEntity.ok(result)
    }

    @GetMapping("/validate")
    fun validate(@RequestHeader("Authorization") token: String): ResponseEntity<*> {
        val bearerToken = token.removePrefix("Bearer ")
        val user = authService.validateToken(bearerToken)
        return if (user != null) {
            ResponseEntity.ok(user)
        } else {
            ResponseEntity.notFound().build<Void>()
        }
    }
}

data class RegisterRequest(
    val email: String,
    val password: String,
    val displayName: String,
    val studentId: String
)

data class LoginRequest(
    val email: String,
    val password: String
)