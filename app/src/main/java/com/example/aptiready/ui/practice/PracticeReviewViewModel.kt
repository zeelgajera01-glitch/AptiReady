package com.example.aptiready.ui.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.aptiready.data.model.PracticeSession
import com.example.aptiready.data.model.SessionSnapshot
import com.example.aptiready.data.repository.AuthRepository
import com.example.aptiready.data.repository.MockTestRepository
import com.example.aptiready.data.repository.PracticeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** The shared review screen accepts a completed practice session or mock attempt. */
class PracticeReviewViewModel(
    private val sessionId: String,
    private val practiceRepository: PracticeRepository,
    private val authRepository: AuthRepository,
    private val mockTestRepository: MockTestRepository
) : ViewModel() {
    val currentOwnerId: String
        get() = authRepository.currentUserId ?: "guest"

    private val _session = MutableStateFlow<PracticeSession?>(null)
    val session: StateFlow<PracticeSession?> = _session.asStateFlow()
    private val _allSnapshots = MutableStateFlow<List<SessionSnapshot>>(emptyList())
    val allSnapshots: StateFlow<List<SessionSnapshot>> = _allSnapshots.asStateFlow()
    private val _filteredSnapshots = MutableStateFlow<List<SessionSnapshot>>(emptyList())
    val filteredSnapshots: StateFlow<List<SessionSnapshot>> = _filteredSnapshots.asStateFlow()
    private val _reviewTitle = MutableStateFlow("Answer review")
    val reviewTitle: StateFlow<String> = _reviewTitle.asStateFlow()
    private val bookmarkActions = com.example.aptiready.ui.common.SessionActions(viewModelScope)
    val actionError = bookmarkActions.error
    private var currentFilter = "all"
    private var loadedOwnerId: String? = null

    init {
        viewModelScope.launch {
            val ownerId = currentOwnerId
            val practice = practiceRepository.getSessionById(sessionId)
            val source: Flow<List<SessionSnapshot>>
            if (practice != null) {
                if (practice.ownerId != ownerId || practice.status != "COMPLETED") return@launch
                _session.value = practice
                _reviewTitle.value = practice.topicTitle
                source = practiceRepository.getSessionSnapshotsFlow(sessionId)
            } else {
                val attempt = mockTestRepository.getAttemptById(sessionId) ?: return@launch
                if (attempt.ownerId != ownerId || attempt.status != "COMPLETED") return@launch
                _reviewTitle.value = attempt.testTitle
                source = mockTestRepository.getAttemptSnapshotsFlow(sessionId).map { list ->
                    list.map { item ->
                        SessionSnapshot(
                            id = item.id,
                            sessionId = item.attemptId,
                            questionId = item.questionId,
                            position = item.position,
                            questionText = item.questionText,
                            options = item.options,
                            correctOptionId = item.correctOptionId,
                            explanation = item.explanation,
                            hint = item.hint,
                            selectedOptionId = item.selectedOptionId,
                            isSubmitted = true,
                            isBookmarked = item.isBookmarked
                        )
                    }
                }
            }
            loadedOwnerId = ownerId
            // The bookmark table is authoritative for both review types.
            combine(source, practiceRepository.getBookmarksForOwner(ownerId)) { snapshots, bookmarks ->
                val saved = bookmarks.map { it.questionId }.toSet()
                snapshots.map { it.copy(isBookmarked = it.questionId in saved) }
            }.collectLatest { list ->
                _allSnapshots.value = if (currentOwnerId == ownerId) list else emptyList()
                applyFilter()
            }
        }
    }

    fun filterSnapshots(filter: String) {
        currentFilter = filter
        applyFilter()
    }

    private fun applyFilter() {
        val all = _allSnapshots.value
        _filteredSnapshots.value = when (currentFilter) {
            "correct" -> all.filter { it.isCorrect() }
            "incorrect" -> all.filter { it.isIncorrect() }
            "skipped" -> all.filter { !it.isSubmitted || it.selectedOptionId == null }
            else -> all
        }
    }

    fun toggleBookmark(snapshot: SessionSnapshot) {
        if (currentOwnerId != loadedOwnerId || _allSnapshots.value.none { it.id == snapshot.id }) return
        val ownerId = currentOwnerId
        bookmarkActions.submit(key = snapshot.questionId) {
            practiceRepository.toggleBookmark(ownerId, snapshot, _reviewTitle.value)
        }
    }

    class Factory(
        private val sessionId: String,
        private val practiceRepository: PracticeRepository,
        private val authRepository: AuthRepository,
        private val mockTestRepository: MockTestRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PracticeReviewViewModel(sessionId, practiceRepository, authRepository, mockTestRepository) as T
    }
}
