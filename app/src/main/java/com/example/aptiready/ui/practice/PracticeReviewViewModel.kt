package com.example.aptiready.ui.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.aptiready.data.model.PracticeSession
import com.example.aptiready.data.model.SessionSnapshot
import com.example.aptiready.data.repository.AuthRepository
import com.example.aptiready.data.repository.PracticeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PracticeReviewViewModel(
    private val sessionId: String,
    private val practiceRepository: PracticeRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    val currentOwnerId: String
        get() = authRepository.currentUserId ?: "guest"

    private val _session = MutableStateFlow<PracticeSession?>(null)
    val session: StateFlow<PracticeSession?> = _session.asStateFlow()

    private val _allSnapshots = MutableStateFlow<List<SessionSnapshot>>(emptyList())
    
    private val _filteredSnapshots = MutableStateFlow<List<SessionSnapshot>>(emptyList())
    val filteredSnapshots: StateFlow<List<SessionSnapshot>> = _filteredSnapshots.asStateFlow()

    private var currentFilter = "all"

    init {
        loadSession()
    }

    private fun loadSession() {
        viewModelScope.launch {
            _session.value = practiceRepository.getSessionById(sessionId)
        }

        viewModelScope.launch {
            practiceRepository.getSessionSnapshotsFlow(sessionId).collectLatest { list ->
                _allSnapshots.value = list
                applyFilter(currentFilter)
            }
        }
    }

    fun filterSnapshots(filter: String) {
        currentFilter = filter
        applyFilter(filter)
    }

    private fun applyFilter(filter: String) {
        val all = _allSnapshots.value
        _filteredSnapshots.value = when (filter) {
            "correct" -> all.filter { it.isCorrect() }
            "incorrect" -> all.filter { it.isIncorrect() }
            "skipped" -> all.filter { !it.isSubmitted || it.selectedOptionId == null }
            else -> all
        }
    }

    fun toggleBookmark(snapshot: SessionSnapshot) {
        val topicTitle = _session.value?.topicTitle ?: "Practice"
        viewModelScope.launch {
            practiceRepository.toggleBookmark(currentOwnerId, snapshot, topicTitle)
        }
    }

    class Factory(
        private val sessionId: String,
        private val practiceRepository: PracticeRepository,
        private val authRepository: AuthRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PracticeReviewViewModel(sessionId, practiceRepository, authRepository) as T
        }
    }
}