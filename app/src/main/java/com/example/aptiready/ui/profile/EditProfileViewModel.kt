package com.example.aptiready.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.aptiready.data.model.ProfileState
import com.example.aptiready.data.model.UserProfile
import com.example.aptiready.data.repository.AuthRepository
import com.example.aptiready.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EditProfileViewModel(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    val profileState: StateFlow<ProfileState> = profileRepository.profileState
    val currentUserEmail: String = authRepository.currentUserEmail ?: ""
    val isVerified: Boolean = authRepository.authState.value is com.example.aptiready.data.model.AuthState.SignedInVerified

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _saveError = MutableStateFlow<String?>(null)
    val saveError: StateFlow<String?> = _saveError.asStateFlow()

    fun loadProfile() {
        val uid = authRepository.currentUserId ?: return
        viewModelScope.launch {
            profileRepository.loadProfile(uid)
        }
    }

    fun saveProfile(displayName: String, dailyGoalText: String, onSuccess: () -> Unit) {
        val uid = authRepository.currentUserId
        if (uid == null) {
            _saveError.value = "No active user session."
            return
        }

        val cleanName = displayName.trim()
        if (cleanName.length !in 1..50) {
            _saveError.value = "Display name must be between 1 and 50 characters."
            return
        }

        val goal = dailyGoalText.toIntOrNull() ?: 10
        if (goal !in 5..100) {
            _saveError.value = "Daily question goal must be between 5 and 100."
            return
        }

        _isSaving.value = true
        _saveError.value = null

        viewModelScope.launch {
            val result = profileRepository.updateProfile(uid, cleanName, goal)
            _isSaving.value = false
            result.onSuccess {
                onSuccess()
            }.onFailure { err ->
                _saveError.value = err.localizedMessage ?: "Failed to update profile."
            }
        }
    }

    class Factory(
        private val authRepository: AuthRepository,
        private val profileRepository: ProfileRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return EditProfileViewModel(authRepository, profileRepository) as T
        }
    }
}