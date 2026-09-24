package com.example.aptiready.ui.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.aptiready.data.model.Topic
import com.example.aptiready.data.repository.QuestionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PracticeViewModel(
    private val questionRepository: QuestionRepository
) : ViewModel() {

    private val _selectedCategoryId = MutableStateFlow("all")
    val selectedCategoryId: StateFlow<String> = _selectedCategoryId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _topics = MutableStateFlow<List<Topic>>(emptyList())
    val topics: StateFlow<List<Topic>> = _topics.asStateFlow()

    init {
        loadTopics()
    }

    fun setCategoryFilter(categoryId: String) {
        _selectedCategoryId.value = categoryId
        loadTopics()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        loadTopics()
    }

    private fun loadTopics() {
        viewModelScope.launch {
            questionRepository.getTopics(
                categoryId = _selectedCategoryId.value,
                searchQuery = _searchQuery.value
            ).collect { list ->
                _topics.value = list
            }
        }
    }

    class Factory(
        private val questionRepository: QuestionRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PracticeViewModel(questionRepository) as T
        }
    }
}