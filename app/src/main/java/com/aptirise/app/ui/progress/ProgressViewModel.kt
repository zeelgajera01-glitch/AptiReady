package com.aptirise.app.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aptirise.app.data.model.UserProgress
import com.aptirise.app.data.repository.LearningOwner
import com.aptirise.app.data.repository.ProgressRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ProgressViewModel(
    private val progressRepository: ProgressRepository,
    private val learningOwner: LearningOwner
) : ViewModel() {

    private val _realProgress = MutableStateFlow<UserProgress?>(null)
    val realProgress: StateFlow<UserProgress?> = _realProgress.asStateFlow()

    init {
        observeProgress()
    }

    private fun observeProgress() {
        viewModelScope.launch {
            kotlinx.coroutines.coroutineScope {
                progressRepository.getUserProgress(includeSampleData = false).collectLatest { progress ->
                    _realProgress.value = progress
                }
            }
        }
    }

    class Factory(
        private val progressRepository: ProgressRepository,
        private val learningOwner: LearningOwner
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ProgressViewModel(progressRepository, learningOwner) as T
        }
    }
}