package com.example.aptiready.ui.tests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.aptiready.data.model.MockTest
import com.example.aptiready.data.model.MockTestAttempt
import com.example.aptiready.data.repository.AuthRepository
import com.example.aptiready.data.repository.MockTestRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TestCatalogViewModel(
    private val mockTestRepository: MockTestRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    val currentOwnerId: String
        get() = authRepository.currentUserId ?: "guest"

    private val _mockTests = MutableStateFlow<List<MockTest>>(emptyList())
    val mockTests: StateFlow<List<MockTest>> = _mockTests.asStateFlow()

    private val _inProgressAttempt = MutableStateFlow<MockTestAttempt?>(null)
    val inProgressAttempt: StateFlow<MockTestAttempt?> = _inProgressAttempt.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            mockTestRepository.getMockTestsFlow().collectLatest { list ->
                _mockTests.value = list
            }
        }

        viewModelScope.launch {
            mockTestRepository.getInProgressAttemptFlow(currentOwnerId).collectLatest { attempt ->
                _inProgressAttempt.value = attempt
            }
        }
    }

    class Factory(
        private val mockTestRepository: MockTestRepository,
        private val authRepository: AuthRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TestCatalogViewModel(mockTestRepository, authRepository) as T
        }
    }
}