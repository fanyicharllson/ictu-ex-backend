package com.fanyiadrien.ictu_ex.data.remote.spring

import com.fanyiadrien.ictu_ex.data.remote.api.AuthResult
import com.fanyiadrien.ictu_ex.data.remote.api.AuthService
import com.fanyiadrien.ictu_ex.data.remote.api.AuthUser
import javax.inject.Inject

/**
 * STEP 3 — SPRING BOOT AUTH IMPLEMENTATION
 *
 * Calls the live API at https://api.ictuex.teamnest.me
 * Stores the JWT in TokenStore for use in subsequent requests.
 */
class SpringAuthService @Inject constructor(
    private val api: IctuExApiService,
    private val tokenStore: TokenStore
) : AuthService {

    override suspend fun register(
        email: String,
        password: String,
        displayName: String,
        studentId: String
    ): Result<AuthResult> = runCatching {
        val result = api.register(RegisterRequest(email, password, displayName, studentId))
        tokenStore.saveToken(result.token)
        result
    }

    override suspend fun login(email: String, password: String): Result<AuthResult> = runCatching {
        val result = api.login(LoginRequest(email, password))
        tokenStore.saveToken(result.token)
        result
    }

    override suspend fun logout(): Result<Unit> = runCatching {
        val bearer = tokenStore.getBearerToken() ?: return@runCatching
        api.logout(bearer)
        tokenStore.clearToken()
    }

    override suspend fun getCurrentUser(): AuthUser? {
        val bearer = tokenStore.getBearerToken() ?: return null
        return runCatching { api.validateToken(bearer) }.getOrNull()
    }

    override suspend fun verifyCode(email: String, code: String): Result<Unit> = runCatching {
        api.verifyCode(VerifyCodeRequest(email, code))
        Unit
    }

    override suspend fun resendVerificationCode(email: String): Result<Unit> = runCatching {
        api.resendVerificationCode(ResendTokenRequest(email))
        Unit
    }

    override suspend fun updateUserType(userType: String): Result<AuthUser> = runCatching {
        val bearer = tokenStore.getBearerToken()
            ?: throw IllegalStateException("Not authenticated")
        api.updateUserType(bearer, UpdateUserTypeRequest(userType))
    }
}
