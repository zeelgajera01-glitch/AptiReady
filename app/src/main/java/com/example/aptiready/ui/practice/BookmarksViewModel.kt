package com.example.aptiready.ui.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.aptiready.data.model.BookmarkItem
import com.example.aptiready.data.repository.AuthRepository
import com.example.aptiready.data.repository.PracticeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class BookmarksViewModel(
    private val practiceRepository: PracticeRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    val currentOwnerId: String
        get() = authRepository.currentUserId ?: "guest"

    private val _bookmarks = MutableStateFlow<List<BookmarkItem>>(emptyList())
    val bookmarks: StateFlow<List<BookmarkItem>> = _bookmarks.asStateFlow()

    init {
        loadBookmarks()
    }

    fun loadBookmarks() {
        viewModelScope.launch {
            practiceRepository.getBookmarksForOwner(currentOwnerId).collectLatest { list ->
                _bookmarks.value = list
            }
        }
    }

    fun removeBookmark(questionId: String) {
        viewModelScope.launch {
            practiceRepository.removeBookmark(currentOwnerId, questionId)
        }
    }

    class Factory(
        private val practiceRepository: PracticeRepository,
        private val authRepository: AuthRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BookmarksViewModel(practiceRepository, authRepository) as T
        }
    }
}