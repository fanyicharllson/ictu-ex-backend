package com.fanyiadrien.ictu_ex.data.remote.spring

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists the JWT token received from Spring Boot login/register.
 * Uses EncryptedSharedPreferences in production — plain prefs here for simplicity.
 * Replace with EncryptedSharedPreferences for production security.
 */
@Singleton
class TokenStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("ictuex_prefs", Context.MODE_PRIVATE)

    fun saveToken(token: String) = prefs.edit { putString(KEY_TOKEN, token) }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun getBearerToken(): String? = getToken()?.let { "Bearer $it" }

    fun clearToken() = prefs.edit { remove(KEY_TOKEN) }

    fun isLoggedIn() = getToken() != null

    companion object {
        private const val KEY_TOKEN = "jwt_token"
    }
}
