package com.example.aptiready.ui.tests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.aptiready.data.model.MockQuestionSnapshot
import com.example.aptiready.data.model.MockTestAttempt
import com.example.aptiready.data.repository.AuthRepository
import com.example.aptiready.data.repository.MockTestRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MockTestViewModel(
    private val attemptId: String,
    private val mockTestRepository: MockTestRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    val currentOwnerId: String
        get() = authRepository.currentUserId ?: "guest"

    private val _attempt = MutableStateFlow<MockTestAttempt?>(null)
    val attempt: StateFlow<MockTestAttempt?> = _attempt.asStateFlow()

    private val _snapshots = MutableStateFlow<List<MockQuestionSnapshot>>(emptyList())
    val snapshots: StateFlow<List<MockQuestionSnapshot>> = _snapshots.asStateFlow()

    private val _currentPosition = MutableStateFlow(0)
    val currentPosition: StateFlow<Int> = _currentPosition.asStateFlow()

    private val _remainingSeconds = MutableStateFlow(0)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

    private val _isExpired = MutableStateFlow(false)
    val isExpired: StateFlow<Boolean> = _isExpired.asStateFlow()

    private var timerJob: Job? = null

    init {
        loadAttempt()
    }

    private fun loadAttempt() {
        viewModelScope.launch {
            val a = mockTestRepository.getAttemptById(attemptId)
            _attempt.value = a
            a?.let {
                _currentPosition.value = it.currentQuestionIndex
                _remainingSeconds.value = it.remainingSeconds
                if (it.status == "IN_PROGRESS" && it.remainingSeconds > 0) {
                    startTimer()
                } else if (it.remainingSeconds <= 0 && it.status == "IN_PROGRESS") {
                    finalizeAttempt("TIME_EXPIRED") {}
                }
            }
        }

        viewModelScope.launch {
            mockTestRepository.getAttemptSnapshotsFlow(attemptId).collectLatest { list ->
                _snapshots.value = list
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_remainingSeconds.value > 0) {
                delay(1000)
                val newRemaining = _remainingSeconds.value - 1
                _remainingSeconds.value = newRemaining

                val totalDuration = _attempt.value?.durationSeconds ?: 900
                val elapsed = totalDuration - newRemaining
                mockTestRepository.saveTimerProgress(attemptId, newRemaining, elapsed)

                if (newRemaining <= 0) {
                    _isExpired.value = true
                    finalizeAttempt("TIME_EXPIRED") {}
                    break
                }
            }
        }
    }

    fun selectOption(optionId: String?) {
        val list = _snapshots.value
        val pos = _currentPosition.value
        if (pos in list.indices) {
            viewModelScope.launch {
                mockTestRepository.saveOptionSelection(attemptId, pos, optionId)
            }
        }
    }

    fun toggleMarkForReview() {
        val list = _snapshots.value
        val pos = _currentPosition.value
        if (pos in list.indices) {
            viewModelScope.launch {
                mockTestRepository.toggleMarkForReview(attemptId, pos)
            }
        }
    }

    fun navigateToPosition(position: Int) {
        val list = _snapshots.value
        if (position in list.indices) {
            _currentPosition.value = position
            viewModelScope.launch {
                mockTestRepository.markPositionVisited(attemptId, position)
                mockTestRepository.updateCurrentPosition(attemptId, position)
            }
        }
    }

    fun finalizeAttempt(reason: String, onFinalized: (MockTestAttempt) -> Unit) {
        timerJob?.cancel()
        viewModelScope.launch {
            val finalized = mockTestRepository.finalizeAttempt(attemptId, reason)
            if (finalized != null) {
                onFinalized(finalized)
            }
        }
    }

    fun discardAttempt(onDiscarded: () -> Unit) {
        timerJob?.cancel()
        viewModelScope.launch {
            mockTestRepository.discardAttempt(attemptId)
            onDiscarded()
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    class Factory(
        private val attemptId: String,
        private val mockTestRepository: MockTestRepository,
        private val authRepository: AuthRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MockTestViewModel(attemptId, mockTestRepository, authRepository) as T
        }
    }
}