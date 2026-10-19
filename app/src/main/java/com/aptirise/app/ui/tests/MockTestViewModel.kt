package com.aptirise.app.ui.tests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aptirise.app.data.model.MockQuestionSnapshot
import com.aptirise.app.data.model.MockTestAttempt
import com.aptirise.app.data.repository.LearningOwner
import com.aptirise.app.data.repository.MockTestRepository
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
    private val learningOwner: LearningOwner
) : ViewModel() {

    val currentOwnerId: String
        get() = learningOwner.ownerId

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
    private val actions = com.aptirise.app.ui.common.SessionActions(viewModelScope)
    val actionError = actions.error
    val isFinishing = actions.busy

    init {
        loadAttempt()
    }

    private fun loadAttempt() {
        viewModelScope.launch {
            val a = try {
                requireNotNull(mockTestRepository.getAttemptById(attemptId))
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                actions.reportError("Could not load this mock test. Please go back and try again.")
                return@launch
            }
            _attempt.value = a
            a?.let {
                _currentPosition.value = it.currentQuestionIndex.coerceIn(0, (it.snapshots.size - 1).coerceAtLeast(0))
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
                actions.submit(key = "timer") {
                    mockTestRepository.saveTimerProgress(attemptId, newRemaining, elapsed)
                }

                if (newRemaining <= 0) {
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
            actions.submit {
                mockTestRepository.saveOptionSelection(attemptId, pos, optionId)
            }
        }
    }

    fun toggleMarkForReview() {
        val list = _snapshots.value
        val pos = _currentPosition.value
        if (pos in list.indices) {
            actions.submit(key = "review:$pos") {
                mockTestRepository.toggleMarkForReview(attemptId, pos)
            }
        }
    }

    fun navigateToPosition(position: Int) {
        val list = _snapshots.value
        if (position in list.indices) {
            _currentPosition.value = position
            actions.submit {
                mockTestRepository.markPositionVisited(attemptId, position)
                mockTestRepository.updateCurrentPosition(attemptId, position)
            }
        }
    }

    fun finalizeAttempt(reason: String, onFinalized: (MockTestAttempt) -> Unit) {
        timerJob?.cancel()
        actions.submit(terminal = true) {
            val finalized = mockTestRepository.finalizeAttempt(attemptId, reason)
            checkNotNull(finalized) { "Attempt is unavailable" }
            _attempt.value = finalized
            _isExpired.value = reason == "TIME_EXPIRED"
            onFinalized(finalized)
        }
    }

    fun saveAndLeave(onSaved: () -> Unit) {
        actions.submit(key = "leave") { onSaved() }
    }

    fun discardAttempt(onDiscarded: () -> Unit) {
        timerJob?.cancel()
        actions.submit(terminal = true) {
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
        private val learningOwner: LearningOwner
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MockTestViewModel(attemptId, mockTestRepository, learningOwner) as T
        }
    }
}