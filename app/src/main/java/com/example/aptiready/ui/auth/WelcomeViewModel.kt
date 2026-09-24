package com.example.aptiready.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.aptiready.data.model.AuthState
import com.example.aptiready.data.repository.AuthRepository
import kotlinx.coroutines.flow.StateFlow

class WelcomeViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    val authState: StateFlow<AuthState> = authRepository.authState
    val isFirebaseConfigured: Boolean = authRepository.isFirebaseConfigured

    class Factory(
        private val authRepository: AuthRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WelcomeViewModel(authRepository) as T
        }
    }
}