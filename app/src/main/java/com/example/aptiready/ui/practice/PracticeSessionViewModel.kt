package com.example.aptiready.ui.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.aptiready.data.model.AuthState
import com.example.aptiready.data.model.FreeHintClaimResult
import com.example.aptiready.data.model.FreeHintStatus
import com.example.aptiready.data.model.PracticeSession
import com.example.aptiready.data.model.SessionSnapshot
import com.example.aptiready.data.repository.AuthRepository
import com.example.aptiready.data.repository.FreeHintRepository
import com.example.aptiready.data.repository.PracticeRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PracticeSessionViewModel(
    val sessionId: String,
    private val practiceRepository: PracticeRepository,
    private val authRepository: AuthRepository,
    private val freeHintRepository: FreeHintRepository
) : ViewModel() {

    val currentOwnerId: String
        get() = authRepository.currentUserId ?: "guest"

    private val _session = MutableStateFlow<PracticeSession?>(null)
    val session: StateFlow<PracticeSession?> = _session.asStateFlow()

    private val _snapshots = MutableStateFlow<List<SessionSnapshot>>(emptyList())
    val snapshots: StateFlow<List<SessionSnapshot>> = _snapshots.asStateFlow()

    private val _currentPosition = MutableStateFlow(0)
    val currentPosition: StateFlow<Int> = _currentPosition.asStateFlow()

    private val _selectedOptionId = MutableStateFlow<String?>(null)
    val selectedOptionId: StateFlow<String?> = _selectedOptionId.asStateFlow()

    private val _freeHintStatus = MutableStateFlow<FreeHintStatus>(FreeHintStatus.Loading)
    val freeHintStatus: StateFlow<FreeHintStatus> = _freeHintStatus.asStateFlow()

    private val _isClaimingFreeHint = MutableStateFlow(false)
    val isClaimingFreeHint: StateFlow<Boolean> = _isClaimingFreeHint.asStateFlow()

    private var hintLookupJob: Job? = null
    private var claimJob: Job? = null
    private var currentLookupContext: String? = null

    init {
        loadSession()
        observeAuthState()
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            authRepository.authState.collectLatest { state ->
                hintLookupJob?.cancel()
                claimJob?.cancel()
                _isClaimingFreeHint.value = false

                when (state) {
                    is AuthState.SignedInVerified -> {
                        val activeQId = _snapshots.value.getOrNull(_currentPosition.value)?.questionId
                        if (activeQId != null) {
                            currentLookupContext = null
                            refreshFreeHintStatus(activeQId, force = true)
                        }
                    }
                    is AuthState.SignedInUnverified -> {
                        currentLookupContext = null
                        _freeHintStatus.value = FreeHintStatus.Unverified("Email verification required to use free hint.")
                    }
                    else -> {
                        currentLookupContext = null
                        _freeHintStatus.value = FreeHintStatus.Unverified("Verified account required to use free hint.")
                    }
                }
            }
        }
    }

    private fun loadSession() {
        viewModelScope.launch {
            val s = practiceRepository.getSessionById(sessionId)
            _session.value = s
            s?.let {
                _currentPosition.value = it.currentQuestionIndex
            }
        }

        viewModelScope.launch {
            practiceRepository.getSessionSnapshotsFlow(sessionId).collectLatest { list ->
                _snapshots.value = list
                if (list.isNotEmpty()) {
                    val pos = _currentPosition.value
                    if (pos in list.indices) {
                        _selectedOptionId.value = list[pos].selectedOptionId
                        val activeQId = list[pos].questionId
                        val activeContext = "$currentOwnerId|$sessionId|$activeQId"
                        if (currentLookupContext != activeContext) {
                            refreshFreeHintStatus(activeQId)
                        }
                    }
                }
            }
        }
    }

    fun refreshFreeHintStatus(questionId: String? = null, force: Boolean = false) {
        val qId = questionId ?: _snapshots.value.getOrNull(_currentPosition.value)?.questionId ?: return
        val activeContext = "$currentOwnerId|$sessionId|$qId"

        if (!force && currentLookupContext == activeContext &&
            (_freeHintStatus.value is FreeHintStatus.UnlockedForCurrent ||
             _freeHintStatus.value is FreeHintStatus.Exhausted ||
             _freeHintStatus.value is FreeHintStatus.Available)
        ) {
            return
        }

        if (_isClaimingFreeHint.value) return

        hintLookupJob?.cancel()
        currentLookupContext = activeContext
        _freeHintStatus.value = FreeHintStatus.Loading

        hintLookupJob = viewModelScope.launch {
            val status = freeHintRepository.getFreeHintStatus(currentOwnerId, sessionId, qId)

            if (_isClaimingFreeHint.value) return@launch
            if (currentLookupContext != activeContext) return@launch

            val activeSnap = _snapshots.value.getOrNull(_currentPosition.value)
            if (activeSnap?.questionId != qId) return@launch

            _freeHintStatus.value = status
        }
    }

    fun handleFreeHintClick() {
        if (_isClaimingFreeHint.value) return
        val loadedSession = _session.value
        if (loadedSession != null && loadedSession.ownerId != currentOwnerId) {
            _freeHintStatus.value = FreeHintStatus.Error("Session owner mismatch.")
            return
        }

        val currentSnap = _snapshots.value.getOrNull(_currentPosition.value) ?: return
        val qId = currentSnap.questionId
        val claimContext = "$currentOwnerId|$sessionId|$qId"

        when (val currentStatus = _freeHintStatus.value) {
            is FreeHintStatus.UnlockedForCurrent -> {
                if (currentStatus.ownerId == currentOwnerId &&
                    currentStatus.sessionId == sessionId &&
                    currentStatus.questionId == qId
                ) {
                    _freeHintStatus.value = currentStatus.copy(isVisible = !currentStatus.isVisible)
                } else {
                    refreshFreeHintStatus(qId, force = true)
                }
            }
            is FreeHintStatus.Available, is FreeHintStatus.Error, is FreeHintStatus.Unverified -> {
                if (currentSnap.hint.isBlank()) {
                    _freeHintStatus.value = FreeHintStatus.Error("No hint available for this question.")
                    return
                }

                hintLookupJob?.cancel()
                claimJob?.cancel()
                _isClaimingFreeHint.value = true

                claimJob = viewModelScope.launch {
                    try {
                        val claimResult = freeHintRepository.claimFreeHint(currentOwnerId, sessionId, qId)

                        val activeSnap = _snapshots.value.getOrNull(_currentPosition.value)
                        if (activeSnap?.questionId != qId) return@launch

                        when (claimResult) {
                            is FreeHintClaimResult.Granted, is FreeHintClaimResult.AllowedReopen -> {
                                currentLookupContext = claimContext
                                _freeHintStatus.value = FreeHintStatus.UnlockedForCurrent(
                                    ownerId = currentOwnerId,
                                    sessionId = sessionId,
                                    questionId = qId,
                                    isVisible = true
                                )
                            }
                            is FreeHintClaimResult.Exhausted -> {
                                currentLookupContext = claimContext
                                _freeHintStatus.value = FreeHintStatus.Exhausted(
                                    claimResult.claimedSessionId,
                                    claimResult.claimedQuestionId
                                )
                            }
                            is FreeHintClaimResult.Unverified -> {
                                _freeHintStatus.value = FreeHintStatus.Unverified(claimResult.reason)
                            }
                            is FreeHintClaimResult.NetworkError -> {
                                _freeHintStatus.value = FreeHintStatus.Error(claimResult.message)
                            }
                        }
                    } finally {
                        if (currentLookupContext == claimContext || currentLookupContext == null) {
                            _isClaimingFreeHint.value = false
                        }
                    }
                }
            }
            else -> {}
        }
    }

    fun selectOption(optionId: String) {
        val list = _snapshots.value
        val pos = _currentPosition.value

        if (pos !in list.indices || list[pos].isSubmitted) {
            return
        }

        // Update the in-memory snapshot immediately so the UI
        // always reflects the currently selected question.
        val updatedList = list.toMutableList()
        updatedList[pos] = updatedList[pos].copy(
            selectedOptionId = optionId
        )

        _snapshots.value = updatedList
        _selectedOptionId.value = optionId

        // Persist the answer asynchronously.
        viewModelScope.launch {
            practiceRepository.saveAnswerSelection(
                sessionId = sessionId,
                position = pos,
                selectedOptionId = optionId
            )
        }
    }

    fun submitCurrentAnswer() {
        val list = _snapshots.value
        val pos = _currentPosition.value
        if (pos in list.indices && !list[pos].isSubmitted && _selectedOptionId.value != null) {
            viewModelScope.launch {
                practiceRepository.submitQuestionAnswer(sessionId, pos)
            }
        }
    }

    fun navigateToPosition(position: Int) {
        val list = _snapshots.value

        if (position !in list.indices) {
            return
        }

        // IMPORTANT:
        // Set the selected answer for the destination question
        // BEFORE changing the current position so the UI cannot
        // render the new question using the previous answer.
        _selectedOptionId.value = list[position].selectedOptionId
        _currentPosition.value = position

        viewModelScope.launch {
            practiceRepository.updateCurrentPosition(
                sessionId,
                position
            )
        }

        val newQId = list[position].questionId
        val activeContext = "$currentOwnerId|$sessionId|$newQId"

        if (currentLookupContext != activeContext) {
            refreshFreeHintStatus(
                newQId,
                force = true
            )
        }
    }

    fun toggleCurrentBookmark() {
        val list = _snapshots.value
        val pos = _currentPosition.value
        val s = _session.value
        if (pos in list.indices && s != null) {
            val snapshot = list[pos]
            viewModelScope.launch {
                practiceRepository.toggleBookmark(currentOwnerId, snapshot, s.topicTitle)
            }
        }
    }

    fun finalizeSession(onFinalized: (PracticeSession) -> Unit) {
        viewModelScope.launch {
            val finalized = practiceRepository.finalizeSession(sessionId, currentOwnerId)
            if (finalized != null) {
                onFinalized(finalized)
            }
        }
    }

    fun discardSession(onDiscarded: () -> Unit) {
        viewModelScope.launch {
            practiceRepository.discardSession(sessionId)
            onDiscarded()
        }
    }

    class Factory(
        private val sessionId: String,
        private val practiceRepository: PracticeRepository,
        private val authRepository: AuthRepository,
        private val freeHintRepository: FreeHintRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PracticeSessionViewModel(sessionId, practiceRepository, authRepository, freeHintRepository) as T
        }
    }
}
