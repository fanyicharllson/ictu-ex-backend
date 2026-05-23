package com.fanyiadrien.ictu_ex.data.remote.api

/**
 * STEP 1 — SERVICE INTERFACES (The Strategy Contracts)
 *
 * These interfaces define WHAT the app can do.
 * They say nothing about HOW (Firebase vs Spring Boot).
 * ViewModels and Repositories only ever depend on these — never on
 * FirebaseAuthService or SpringAuthService directly.
 */

// ─── Models ──────────────────────────────────────────────────────────────────

data class AuthUser(
    val id: String,
    val email: String,
    val displayName: String,
    val studentId: String,
    val userType: String  // STUDENT | BUYER | SELLER
)

data class AuthResult(
    val token: String,
    val user: AuthUser,
    val message: String
)

// ─── Interface ───────────────────────────────────────────────────────────────

interface AuthService {
    /** Register a new ICTU student. Returns token + user on success. */
    suspend fun register(
        email: String,
        password: String,
        displayName: String,
        studentId: String
    ): Result<AuthResult>

    /** Login with email + password. Returns token + user on success. */
    suspend fun login(email: String, password: String): Result<AuthResult>

    /** Logout — invalidates the current session/token. */
    suspend fun logout(): Result<Unit>

    /** Returns the currently authenticated user, or null if not logged in. */
    suspend fun getCurrentUser(): AuthUser?

    /** Verify the 6-digit code sent to the student's email. */
    suspend fun verifyCode(email: String, code: String): Result<Unit>

    /** Resend the verification code to the given email. */
    suspend fun resendVerificationCode(email: String): Result<Unit>

    /** Update the user's type (BUYER or SELLER). */
    suspend fun updateUserType(userType: String): Result<AuthUser>
}
