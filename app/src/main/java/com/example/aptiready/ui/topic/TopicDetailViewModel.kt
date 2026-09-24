package com.example.aptiready.ui.topic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.aptiready.data.model.Difficulty
import com.example.aptiready.data.model.PracticeSession
import com.example.aptiready.data.model.Topic
import com.example.aptiready.data.repository.AuthRepository
import com.example.aptiready.data.repository.PracticeRepository
import com.example.aptiready.data.repository.QuestionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TopicDetailViewModel(
    private val topicId: String,
    private val questionRepository: QuestionRepository,
    private val practiceRepository: PracticeRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    val currentOwnerId: String
        get() = authRepository.currentUserId ?: "guest"

    private val _topic = MutableStateFlow<Topic?>(null)
    val topic: StateFlow<Topic?> = _topic.asStateFlow()

    private val _selectedDifficulty = MutableStateFlow<Difficulty?>(null) // Default: All Difficulties
    val selectedDifficulty: StateFlow<Difficulty?> = _selectedDifficulty.asStateFlow()

    private val _selectedLength = MutableStateFlow(10)
    val selectedLength: StateFlow<Int> = _selectedLength.asStateFlow()

    private val _availableCount = MutableStateFlow(60)
    val availableCount: StateFlow<Int> = _availableCount.asStateFlow()

    private val _unseenCount = MutableStateFlow(60)
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

    fun updateAvailableCount() {
        viewModelScope.launch {
            val total = practiceRepository.getAvailableQuestionCount(topicId, _selectedDifficulty.value)
            val unseen = practiceRepository.getUnseenQuestionCount(currentOwnerId, topicId, _selectedDifficulty.value)
            _availableCount.value = total
            _unseenCount.value = unseen
        }
    }

    fun startPracticeSession(countToRequest: Int, onSessionCreated: (PracticeSession) -> Unit) {
        viewModelScope.launch {
            val actualCount = minOf(countToRequest, _availableCount.value)
            if (actualCount <= 0) return@launch
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
        private val authRepository: AuthRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TopicDetailViewModel(topicId, questionRepository, practiceRepository, authRepository) as T
        }
    }
}