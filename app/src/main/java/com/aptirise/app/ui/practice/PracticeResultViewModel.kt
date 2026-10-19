package com.aptirise.app.ui.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aptirise.app.data.model.PracticeSession
import com.aptirise.app.data.repository.LearningOwner
import com.aptirise.app.data.repository.PracticeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PracticeResultViewModel(
    private val sessionId: String,
    private val practiceRepository: PracticeRepository,
    private val learningOwner: LearningOwner
) : ViewModel() {

    val currentOwnerId: String
        get() = learningOwner.ownerId

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

    private val creationActions = com.aptirise.app.ui.common.SessionActions(viewModelScope)
    val creationBusy = creationActions.busy
    val creationError = creationActions.error
    fun allowNewCreation() = creationActions.reopen()

    fun retryIncorrect(onRetryCreated: (PracticeSession) -> Unit) {
        creationActions.submit(terminal = true) {
            val newSession = practiceRepository.createRetrySession(currentOwnerId, sessionId)
            checkNotNull(newSession) { "No incorrect answers to retry" }
            onRetryCreated(newSession)
        }
    }

    class Factory(
        private val sessionId: String,
        private val practiceRepository: PracticeRepository,
        private val learningOwner: LearningOwner
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PracticeResultViewModel(sessionId, practiceRepository, learningOwner) as T
        }
    }
}