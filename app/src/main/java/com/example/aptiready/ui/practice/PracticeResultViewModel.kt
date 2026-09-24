package com.example.aptiready.ui.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.aptiready.data.model.PracticeSession
import com.example.aptiready.data.repository.AuthRepository
import com.example.aptiready.data.repository.PracticeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PracticeResultViewModel(
    private val sessionId: String,
    private val practiceRepository: PracticeRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    val currentOwnerId: String
        get() = authRepository.currentUserId ?: "guest"

    private val _session = MutableStateFlow<PracticeSession?>(null)
    val session: StateFlow<PracticeSession?> = _session.asStateFlow()

    init {
        loadSession()
    }

    private fun loadSession() {
        viewModelScope.launch {
            _session.value = practiceRepository.getSessionById(sessionId)
        }
    }

    fun retryIncorrect(onRetryCreated: (PracticeSession) -> Unit) {
        viewModelScope.launch {
            val newSession = practiceRepository.createRetrySession(currentOwnerId, sessionId)
            if (newSession != null) {
                onRetryCreated(newSession)
            }
        }
    }

    class Factory(
        private val sessionId: String,
        private val practiceRepository: PracticeRepository,
        private val authRepository: AuthRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PracticeResultViewModel(sessionId, practiceRepository, authRepository) as T
        }
    }
}