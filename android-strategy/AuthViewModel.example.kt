package com.fanyiadrien.ictu_ex.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fanyiadrien.ictu_ex.data.remote.api.AuthResult
import com.fanyiadrien.ictu_ex.data.remote.api.AuthService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * USAGE EXAMPLE — AuthViewModel
 *
 * This ViewModel has NO idea whether it's talking to Firebase or Spring Boot.
 * It only knows about the AuthService interface.
 * Hilt injects whichever implementation was chosen in ServiceModule.
 *
 * Drop this into:  feature/auth/AuthViewModel.kt
 */

sealed class AuthUiState {
    object Idle    : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val result: AuthResult) : AuthUiState()
    data class Error(val message: String)      : AuthUiState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authService: AuthService   // ← interface only, never Firebase/Spring directly
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState

    fun register(email: String, password: String, displayName: String, studentId: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authService.register(email, password, displayName, studentId)
                .onSuccess { _uiState.value = AuthUiState.Success(it) }
                .onFailure { _uiState.value = AuthUiState.Error(it.message ?: "Registration failed") }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authService.login(email, password)
                .onSuccess { _uiState.value = AuthUiState.Success(it) }
                .onFailure { _uiState.value = AuthUiState.Error(it.message ?: "Login failed") }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authService.logout()
            _uiState.value = AuthUiState.Idle
        }
    }

    fun verifyCode(email: String, code: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authService.verifyCode(email, code)
                .onSuccess { _uiState.value = AuthUiState.Idle }
                .onFailure { _uiState.value = AuthUiState.Error(it.message ?: "Verification failed") }
        }
    }
}
