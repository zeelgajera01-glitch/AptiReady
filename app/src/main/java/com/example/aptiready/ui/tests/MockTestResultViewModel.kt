package com.example.aptiready.ui.tests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.aptiready.data.model.MockTestAttempt
import com.example.aptiready.data.repository.AuthRepository
import com.example.aptiready.data.repository.MockTestRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MockTestResultViewModel(
    private val attemptId: String,
    private val mockTestRepository: MockTestRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    val currentOwnerId: String
        get() = authRepository.currentUserId ?: "guest"

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

    fun retakeTest(onAttemptCreated: (MockTestAttempt) -> Unit) {
        val testId = _attempt.value?.testId ?: return
        viewModelScope.launch {
            val result = mockTestRepository.createMockAttempt(currentOwnerId, testId)
            result.onSuccess { newAttempt ->
                onAttemptCreated(newAttempt)
            }
        }
    }

    class Factory(
        private val attemptId: String,
        private val mockTestRepository: MockTestRepository,
        private val authRepository: AuthRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MockTestResultViewModel(attemptId, mockTestRepository, authRepository) as T
        }
    }
}