package com.example.aptiready.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.aptiready.data.repository.AuthRepository
import com.example.aptiready.data.repository.CloudSyncRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AccountDeletionViewModel(
    private val authRepository: AuthRepository,
    private val cloudSyncRepository: CloudSyncRepository
) : ViewModel() {

    private val _isDeleting = MutableStateFlow(false)
    val isDeleting: StateFlow<Boolean> = _isDeleting.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun deleteAccount(onSuccess: () -> Unit) {
        _isDeleting.value = true
        _error.value = null

        viewModelScope.launch {
            val uid = authRepository.currentUserId
            val firebaseUser = FirebaseAuth.getInstance().currentUser

            if (uid == null || firebaseUser == null) {
                _isDeleting.value = false
                _error.value = "No active user session."
                return@launch
            }

            try {
                // Call Cloud Function requestAccountDeletion or delete user
                try {
                    val functions = FirebaseFunctions.getInstance()
                    functions.getHttpsCallable("requestAccountDeletion").call().await()
                } catch (_: Exception) {
                    // Direct Auth user deletion if function is offline
                    firebaseUser.delete().await()
                }

                // Clear local outbox and user data
                cloudSyncRepository.clearOutboxForOwner(uid)
                authRepository.signOut()

                _isDeleting.value = false
                onSuccess()
            } catch (e: Exception) {
                _isDeleting.value = false
                _error.value = e.localizedMessage ?: "Account deletion failed."
            }
        }
    }

    class Factory(
        private val authRepository: AuthRepository,
        private val cloudSyncRepository: CloudSyncRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AccountDeletionViewModel(authRepository, cloudSyncRepository) as T
        }
    }
}