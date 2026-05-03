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
}

data class AuthResult(
    val token: String,
    val user: AuthUser,
    val message: String
)