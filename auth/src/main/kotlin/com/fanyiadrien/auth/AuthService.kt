package com.fanyiadrien.auth

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
}

data class AuthResult(
    val token: String,
    val user: AuthUser,
    val message: String
)