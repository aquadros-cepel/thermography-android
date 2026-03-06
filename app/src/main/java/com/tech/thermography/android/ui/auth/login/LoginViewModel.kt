package com.tech.thermography.android.ui.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tech.thermography.android.data.local.storage.UserSessionStore
import com.tech.thermography.android.data.remote.auth.AuthRepository
import com.tech.thermography.android.util.JwtUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val sessionStore: UserSessionStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()

    init {
        checkSession()
    }

    private fun checkSession() = viewModelScope.launch {
        // Busca o token salvo no DataStore
        val token = sessionStore.token.first()
        
        // Se o token existe e não está expirado, autentica automaticamente
        if (!token.isNullOrBlank() && !JwtUtils.isExpired(token)) {
            _uiState.update { it.copy(isAuthenticated = true) }
        } else {
            // Se expirou ou não existe, garante que a sessão esteja limpa
            sessionStore.clearSession()
        }
    }

    fun onUsernameChanged(v: String) {
        _uiState.update { it.copy(username = v) }
    }

    fun onPasswordChanged(v: String) {
        _uiState.update { it.copy(password = v) }
    }

    fun onRememberChanged(v: Boolean) {
        _uiState.update { it.copy(rememberMe = v) }
    }

    fun login() = viewModelScope.launch {
        val current = _uiState.value
        if (current.username.isBlank()) {
            _uiState.update { it.copy(error = "E-mail obrigatório") }
            return@launch
        }
        if (current.password.length < 5) {
            _uiState.update { it.copy(error = "Senha deve ter pelo menos 5 caracteres") }
            return@launch
        }

        _uiState.update { it.copy(loading = true, error = null) }

        val result = repository.login(current.username, current.password, current.rememberMe)
        if (result.success) {
            _uiState.update { it.copy(isAuthenticated = true, loading = false) }
        } else {
            _uiState.update { it.copy(error = result.error ?: "Erro desconhecido", loading = false) }
        }
    }
}
