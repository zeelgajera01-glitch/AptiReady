package com.example.aptiready.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.aptiready.data.repository.AuthRepository

class ForgotPasswordViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    class Factory(
        private val authRepository: AuthRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ForgotPasswordViewModel(authRepository) as T
        }
    }
}