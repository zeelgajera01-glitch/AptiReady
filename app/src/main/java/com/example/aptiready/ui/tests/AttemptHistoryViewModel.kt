package com.example.aptiready.ui.tests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.aptiready.data.model.AttemptHistoryItem
import com.example.aptiready.data.repository.AuthRepository
import com.example.aptiready.data.repository.MockTestRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AttemptHistoryViewModel(
    private val mockTestRepository: MockTestRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    val currentOwnerId: String
        get() = authRepository.currentUserId ?: "guest"

    private val _allHistory = MutableStateFlow<List<AttemptHistoryItem>>(emptyList())
    private val _filteredHistory = MutableStateFlow<List<AttemptHistoryItem>>(emptyList())
    val filteredHistory: StateFlow<List<AttemptHistoryItem>> = _filteredHistory.asStateFlow()

    private var currentFilter = "all"

    init {
        loadHistory()
    }

    fun loadHistory() {
        viewModelScope.launch {
            mockTestRepository.getUnifiedAttemptHistory(currentOwnerId).collectLatest { list ->
                _allHistory.value = list
                applyFilter(currentFilter)
            }
        }
    }

    fun filterHistory(filter: String) {
        currentFilter = filter
        applyFilter(filter)
    }

    private fun applyFilter(filter: String) {
        val all = _allHistory.value
        _filteredHistory.value = when (filter) {
            "practice" -> all.filter { it.type == "PRACTICE" }
            "mock" -> all.filter { it.type == "MOCK_TEST" }
            else -> all
        }
    }

    class Factory(
        private val mockTestRepository: MockTestRepository,
        private val authRepository: AuthRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AttemptHistoryViewModel(mockTestRepository, authRepository) as T
        }
    }
}