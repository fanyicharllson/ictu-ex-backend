package com.fanyiadrien.auth

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

interface AuthService {
    fun register(
        email: String,
        password: String,
        displayName: String,
        studentId: String
    ): AuthResult

    fun login(email: String, password: String): AuthResult

    fun validateToken(token: String): AuthUser?

    fun updateUserType(token: String, userType: String): AuthUser

    fun getUserById(userId: UUID): AuthUser?

    fun verifyCode(email: String, code: String): Boolean

    fun resendVerificationCode(email: String)

    fun logout(token: String)

    // Token inspection methods — exposed so ictu-ex-app filter does not depend on internal JwtService
    fun isTokenValid(token: String): Boolean
    fun extractTokenJti(token: String): String?
    fun extractTokenUserId(token: String): UUID
}

data class AuthResult(
    @field:Schema(description = "JWT access token")
    val token: String,
    @field:Schema(description = "Authenticated user profile")
    val user: AuthUser,
    @field:Schema(description = "Authentication result message")
    val message: String
)