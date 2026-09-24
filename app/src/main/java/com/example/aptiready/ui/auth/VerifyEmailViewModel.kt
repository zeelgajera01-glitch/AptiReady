package com.example.aptiready.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.aptiready.data.repository.AuthRepository
import com.example.aptiready.data.repository.ProfileRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VerifyEmailViewModel(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    val currentUserEmail: String = authRepository.currentUserEmail ?: ""

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _cooldownSeconds = MutableStateFlow(0)
    val cooldownSeconds: StateFlow<Int> = _cooldownSeconds.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private var cooldownJob: Job? = null

    fun checkVerification(onVerified: () -> Unit, onUnverified: (String) -> Unit) {
        _isLoading.value = true
        _statusMessage.value = null

        viewModelScope.launch {
            val result = authRepository.reloadUserAndCheckVerification()
            _isLoading.value = false
            result.onSuccess { isVerified ->
                if (isVerified) {
                    val uid = authRepository.currentUserId ?: ""
                    val email = authRepository.currentUserEmail ?: ""
                    profileRepository.ensureProfileExists(uid, email.substringBefore("@"))
                    onVerified()
                } else {
                    onUnverified("Email is not verified yet. Please check your inbox or click 'Resend' to receive a new link.")
                }
            }.onFailure { err ->
                onUnverified(err.localizedMessage ?: "Failed to verify email status.")
            }
        }
    }

    fun resendVerification() {
        if (_cooldownSeconds.value > 0) return

        _isLoading.value = true
        _statusMessage.value = null

        viewModelScope.launch {
            val result = authRepository.sendEmailVerification()
            _isLoading.value = false
            result.onSuccess {
                _statusMessage.value = "Verification email sent. Check your inbox!"
                startCooldownTimer(60)
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "Failed to resend verification email."
            }
        }
    }

    private fun startCooldownTimer(seconds: Int) {
        cooldownJob?.cancel()
        _cooldownSeconds.value = seconds
        cooldownJob = viewModelScope.launch {
            while (_cooldownSeconds.value > 0) {
                delay(1000)
                _cooldownSeconds.value -= 1
            }
        }
    }

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