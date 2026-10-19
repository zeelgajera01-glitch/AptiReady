package com.aptirise.app.ui.topic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aptirise.app.data.model.Difficulty
import com.aptirise.app.data.model.PracticeSession
import com.aptirise.app.data.model.Topic
import com.aptirise.app.data.repository.LearningOwner
import com.aptirise.app.data.repository.PracticeRepository
import com.aptirise.app.data.repository.QuestionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TopicDetailViewModel(
    private val topicId: String,
    private val questionRepository: QuestionRepository,
    private val practiceRepository: PracticeRepository,
    private val learningOwner: LearningOwner
) : ViewModel() {

    val currentOwnerId: String
        get() = learningOwner.ownerId

    private val _topic = MutableStateFlow<Topic?>(null)
    val topic: StateFlow<Topic?> = _topic.asStateFlow()

    private val _selectedDifficulty = MutableStateFlow<Difficulty?>(null) // Default: All Difficulties
    val selectedDifficulty: StateFlow<Difficulty?> = _selectedDifficulty.asStateFlow()

    private val _selectedLength = MutableStateFlow(10)
    val selectedLength: StateFlow<Int> = _selectedLength.asStateFlow()

    private val _availableCount = MutableStateFlow(0)
    val availableCount: StateFlow<Int> = _availableCount.asStateFlow()

    private val _unseenCount = MutableStateFlow(0)
    val unseenCount: StateFlow<Int> = _unseenCount.asStateFlow()

    init {
        loadTopic()
    }

    private fun loadTopic() {
        viewModelScope.launch {
            questionRepository.getTopicById(topicId).collect { loadedTopic ->
                _topic.value = loadedTopic
                updateAvailableCount()
            }
        }
    }

    fun setDifficulty(difficulty: Difficulty?) {
        _selectedDifficulty.value = difficulty
        updateAvailableCount()
    }

    fun setPracticeLength(length: Int) {
        _selectedLength.value = length
    }

    private var countJob: kotlinx.coroutines.Job? = null
    fun updateAvailableCount() {
        countJob?.cancel()
        countJob = viewModelScope.launch {
            try {
                val counts = practiceRepository.getTopicQuestionCounts(currentOwnerId, topicId, _selectedDifficulty.value)
                _availableCount.value = counts.available
                _unseenCount.value = counts.unseen
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                creationActions.reportError("Could not load question counts. Please go back and try again.")
            }
        }
    }

    private val creationActions = com.aptirise.app.ui.common.SessionActions(viewModelScope)
    val creationBusy = creationActions.busy
    val creationError = creationActions.error
    fun allowNewCreation() = creationActions.reopen()

    fun startPracticeSession(countToRequest: Int, onSessionCreated: (PracticeSession) -> Unit) {
        creationActions.submit(terminal = true) {
            val actualCount = minOf(countToRequest, _availableCount.value)
            check(actualCount > 0) { "Questions are still loading" }
            val session = practiceRepository.createPracticeSession(
                ownerId = currentOwnerId,
                topicId = topicId,
                difficulty = _selectedDifficulty.value,
                requestedCount = actualCount
            )
            onSessionCreated(session)
        }
    }

    class Factory(
        private val topicId: String,
        private val questionRepository: QuestionRepository,
        private val practiceRepository: PracticeRepository,
        private val learningOwner: LearningOwner
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TopicDetailViewModel(topicId, questionRepository, practiceRepository, learningOwner) as T
        }
    }
}