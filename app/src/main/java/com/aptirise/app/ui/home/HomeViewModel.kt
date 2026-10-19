package com.aptirise.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aptirise.app.data.model.PracticeSession
import com.aptirise.app.data.model.Topic
import com.aptirise.app.data.model.UserProgress
import com.aptirise.app.data.repository.LearningOwner
import com.aptirise.app.data.repository.PracticeRepository
import com.aptirise.app.data.repository.ProgressRepository
import com.aptirise.app.data.repository.QuestionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeViewModel(
    private val questionRepository: QuestionRepository,
    private val progressRepository: ProgressRepository,
    private val practiceRepository: PracticeRepository,
    private val learningOwner: LearningOwner
) : ViewModel() {

    val currentOwnerId: String
        get() = learningOwner.ownerId

    private val _demoProgress = MutableStateFlow<UserProgress?>(null)
    val demoProgress: StateFlow<UserProgress?> = _demoProgress.asStateFlow()

    private val _recentTopic = MutableStateFlow<Topic?>(null)
    val recentTopic: StateFlow<Topic?> = _recentTopic.asStateFlow()

    private val _inProgressSession = MutableStateFlow<PracticeSession?>(null)
    val inProgressSession: StateFlow<PracticeSession?> = _inProgressSession.asStateFlow()

    init {
        loadHomeData()
    }

    private fun loadHomeData() {
        viewModelScope.launch {
            kotlinx.coroutines.coroutineScope {
                val uid = currentOwnerId

                launch {
                    progressRepository.getUserProgress(includeSampleData = false).collectLatest { progress ->
                        _demoProgress.value = progress
                    }
                }

                launch {
                    practiceRepository.getInProgressSessionFlow(uid).collectLatest { session ->
                        _inProgressSession.value = session
                    }
                }
            }
        }

        viewModelScope.launch {
            progressRepository.getRecentTopic().collectLatest { topic ->
                _recentTopic.value = topic
            }
        }
    }

    class Factory(
        private val questionRepository: QuestionRepository,
        private val progressRepository: ProgressRepository,
        private val practiceRepository: PracticeRepository,
        private val learningOwner: LearningOwner
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(questionRepository, progressRepository, practiceRepository, learningOwner) as T
        }
    }
}