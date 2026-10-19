package com.aptirise.app.ui.tests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aptirise.app.data.model.MockTestAttempt
import com.aptirise.app.data.repository.LearningOwner
import com.aptirise.app.data.repository.MockTestRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MockTestResultViewModel(
    private val attemptId: String,
    private val mockTestRepository: MockTestRepository,
    private val learningOwner: LearningOwner
) : ViewModel() {

    val currentOwnerId: String
        get() = learningOwner.ownerId

    private val _attempt = MutableStateFlow<MockTestAttempt?>(null)
    val attempt: StateFlow<MockTestAttempt?> = _attempt.asStateFlow()

    init {
        loadAttempt()
    }

    private fun loadAttempt() {
        viewModelScope.launch {
            _attempt.value = mockTestRepository.getAttemptById(attemptId)
        }
    }

    private val creationActions = com.aptirise.app.ui.common.SessionActions(viewModelScope)
    val creationBusy = creationActions.busy
    val creationError = creationActions.error
    fun allowNewCreation() = creationActions.reopen()

    fun retakeTest(onAttemptCreated: (MockTestAttempt) -> Unit) {
        val testId = _attempt.value?.testId ?: return
        creationActions.submit(terminal = true) {
            val result = mockTestRepository.createMockAttempt(currentOwnerId, testId)
            result.getOrThrow()
            result.onSuccess { newAttempt ->
                onAttemptCreated(newAttempt)
            }
        }
    }

    class Factory(
        private val attemptId: String,
        private val mockTestRepository: MockTestRepository,
        private val learningOwner: LearningOwner
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MockTestResultViewModel(attemptId, mockTestRepository, learningOwner) as T
        }
    }
}