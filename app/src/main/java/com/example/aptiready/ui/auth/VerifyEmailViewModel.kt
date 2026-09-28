package com.example.aptiready.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.aptiready.data.repository.AuthRepository
import com.example.aptiready.data.repository.ProfileRepository

class VerifyEmailViewModel(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    val currentUserEmail: String = authRepository.currentUserEmail ?: ""

    fun signOut() {
        authRepository.signOut()
    }

    class Factory(
        private val authRepository: AuthRepository,
        private val profileRepository: ProfileRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return VerifyEmailViewModel(authRepository, profileRepository) as T
        }
    }
}