package com.example.aptiready.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.aptiready.data.model.UserProgress
import com.example.aptiready.data.repository.AuthRepository
import com.example.aptiready.data.repository.ProgressRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ProgressViewModel(
    private val progressRepository: ProgressRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _realProgress = MutableStateFlow<UserProgress?>(null)
    val realProgress: StateFlow<UserProgress?> = _realProgress.asStateFlow()

    init {
        observeProgress()
    }

    private fun observeProgress() {
        viewModelScope.launch {
            authRepository.authState.collectLatest { _ ->
                progressRepository.getUserProgress(includeSampleData = false).collectLatest { progress ->
                    _realProgress.value = progress
                }
            }
        }
    }

    class Factory(
        private val progressRepository: ProgressRepository,
        private val authRepository: AuthRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ProgressViewModel(progressRepository, authRepository) as T
        }
    }
}