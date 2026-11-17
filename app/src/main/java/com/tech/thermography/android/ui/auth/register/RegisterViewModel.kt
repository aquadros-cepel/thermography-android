package com.tech.thermography.android.ui.auth.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tech.thermography.android.data.remote.auth.AuthRepository
import com.tech.thermography.android.data.remote.auth.RegisterRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState = _uiState.asStateFlow()

    fun onFirstNameChanged(v: String) = _uiState.update { it.copy(firstName = v) }
    fun onEmailChanged(v: String) = _uiState.update { it.copy(email = v) }
    fun onPhoneNumberChanged(v: String) = _uiState.update { it.copy(phoneNumber = v) }
    fun onLoginChanged(v: String) = _uiState.update { it.copy(login = v) }
    fun onPasswordChanged(v: String) = _uiState.update { it.copy(password = v) }
    fun onConfirmPasswordChanged(v: String) = _uiState.update { it.copy(confirmPassword = v) }

    fun register() = viewModelScope.launch {
        val state = _uiState.value
        
        if (!validate(state)) return@launch

        _uiState.update { it.copy(loading = true, error = null) }

        val request = RegisterRequest(
            login = state.login,
            firstName = state.firstName,
            lastName = "",
            email = state.email,
            password = state.password,
            langKey = "pt-br",
            phoneNumber = state.phoneNumber,
            activated = true
        )

        val result = repository.register(request)
        if (result.success) {
            _uiState.update { it.copy(loading = false, success = true, successMessage = result.message) }
        } else {
            _uiState.update { it.copy(loading = false, error = result.error ?: "Falha ao criar conta") }
        }
    }

    private fun validate(state: RegisterUiState): Boolean {
        if (state.firstName.isBlank() || state.email.isBlank() || state.phoneNumber.isBlank() || 
            state.login.isBlank() || state.password.isBlank() || state.confirmPassword.isBlank()) {
            _uiState.update { it.copy(error = "Todos os campos são obrigatórios") }
            return false
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(state.email).matches()) {
            _uiState.update { it.copy(error = "E-mail inválido") }
            return false
        }

        if (state.password != state.confirmPassword) {
            _uiState.update { it.copy(error = "Senhas não conferem") }
            return false
        }

        // Pelo menos 6 caracteres, alpha numéricos e um caracter especial
        val passwordRegex = Regex("^(?=.*[a-zA-Z])(?=.*[0-9])(?=.*[^a-zA-Z0-9]).{6,}$")
        if (!passwordRegex.matches(state.password)) {
            _uiState.update { it.copy(error = "Senha deve conter pelo menos 6 caracteres, letras, números e um caractere especial") }
            return false
        }

        return true
    }
}
