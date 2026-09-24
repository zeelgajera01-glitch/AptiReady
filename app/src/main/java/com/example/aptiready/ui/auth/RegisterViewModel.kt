package com.example.aptiready.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.aptiready.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RegisterViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    val isFirebaseConfigured: Boolean = authRepository.isFirebaseConfigured

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _registerError = MutableStateFlow<String?>(null)
    val registerError: StateFlow<String?> = _registerError.asStateFlow()

    fun register(displayName: String, email: String, password: String, confirmPassword: String, onSuccess: () -> Unit) {
        val cleanName = displayName.trim()
        val cleanEmail = email.trim()

        if (cleanName.length !in 1..50) {
            _registerError.value = "Display name must be between 1 and 50 characters."
            return
        }
        if (cleanEmail.isEmpty()) {
            _registerError.value = "Please enter a valid email address."
            return
        }
        if (password.length < 6) {
            _registerError.value = "Password must be at least 6 characters long."
            return
        }
        if (password != confirmPassword) {
            _registerError.value = "Passwords do not match."
            return
        }

        _isLoading.value = true
        _registerError.value = null

        viewModelScope.launch {
            val result = authRepository.registerWithEmail(cleanName, cleanEmail, password)
            _isLoading.value = false
            result.onSuccess {
                onSuccess()
            }.onFailure { exception ->
                _registerError.value = exception.localizedMessage ?: "Registration failed."
            }
        }
    }

    class Factory(
        private val authRepository: AuthRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RegisterViewModel(authRepository) as T
        }
    }
}