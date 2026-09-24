package com.example.aptiready.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.aptiready.data.model.AuthState
import com.example.aptiready.data.model.ProfileState
import com.example.aptiready.data.model.SyncStatus
import com.example.aptiready.data.model.ThemeMode
import com.example.aptiready.data.repository.AuthRepository
import com.example.aptiready.data.repository.CloudSyncRepository
import com.example.aptiready.data.repository.ProfileRepository
import com.example.aptiready.data.repository.ThemeRepository
import com.example.aptiready.ui.ads.AdProvider
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val themeRepository: ThemeRepository,
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val cloudSyncRepository: CloudSyncRepository,
    private val adProvider: AdProvider
) : ViewModel() {

    val authState: StateFlow<AuthState> = authRepository.authState
    val profileState: StateFlow<ProfileState> = profileRepository.profileState
    val syncStatus: StateFlow<SyncStatus> = cloudSyncRepository.syncStatus
    val isBackupEnabled: StateFlow<Boolean> = cloudSyncRepository.isBackupEnabled
    val lastSyncTimestamp: StateFlow<Long> = cloudSyncRepository.lastSyncTimestamp
    val pendingOutboxCount: StateFlow<Int> = cloudSyncRepository.pendingOutboxCount
    val isPrivacyOptionsRequired: StateFlow<Boolean> = adProvider.isPrivacyOptionsRequired

    fun loadProfile() {
        val uid = authRepository.currentUserId
        if (uid != null) {
            viewModelScope.launch {
                profileRepository.loadProfile(uid)
            }
        }
    }

    fun setBackupEnabled(enabled: Boolean) {
        viewModelScope.launch {
            cloudSyncRepository.setBackupEnabled(enabled)
        }
    }

    fun triggerSyncNow() {
        cloudSyncRepository.triggerSyncNow()
    }

    fun retryPermanentFailures() {
        viewModelScope.launch {
            cloudSyncRepository.retryPermanentFailures()
        }
    }

    fun selectTheme(mode: ThemeMode) {
        viewModelScope.launch {
            themeRepository.setThemeMode(mode)
        }
    }

    fun signOut() {
        authRepository.signOut()
    }

    class Factory(
        private val themeRepository: ThemeRepository,
        private val authRepository: AuthRepository,
        private val profileRepository: ProfileRepository,
        private val cloudSyncRepository: CloudSyncRepository,
        private val adProvider: AdProvider
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ProfileViewModel(themeRepository, authRepository, profileRepository, cloudSyncRepository, adProvider) as T
        }
    }
}