package com.example.aptiready.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.aptiready.data.repository.AuthRepository

class RegisterViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    val isFirebaseConfigured: Boolean = authRepository.isFirebaseConfigured

    class Factory(
        private val authRepository: AuthRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RegisterViewModel(authRepository) as T
        }
    }
}