package com.example.aptiready.data.repository

import com.example.aptiready.data.model.BookmarkItem
import com.example.aptiready.data.model.Difficulty
import com.example.aptiready.data.model.PracticeSession
import com.example.aptiready.data.model.SessionSnapshot
import kotlinx.coroutines.flow.Flow

interface PracticeRepository {
    suspend fun getAvailableQuestionCount(topicId: String, difficulty: Difficulty?): Int
    suspend fun getUnseenQuestionCount(ownerId: String, topicId: String, difficulty: Difficulty?): Int
    suspend fun createPracticeSession(ownerId: String, topicId: String, difficulty: Difficulty?, requestedCount: Int): PracticeSession
    suspend fun createRetrySession(ownerId: String, originalSessionId: String): PracticeSession?
    suspend fun getInProgressSession(ownerId: String): PracticeSession?
    fun getInProgressSessionFlow(ownerId: String): Flow<PracticeSession?>
    suspend fun getSessionById(sessionId: String): PracticeSession?
    fun getSessionSnapshotsFlow(sessionId: String): Flow<List<SessionSnapshot>>
    suspend fun saveAnswerSelection(sessionId: String, position: Int, selectedOptionId: String?)
    suspend fun submitQuestionAnswer(sessionId: String, position: Int)
    suspend fun updateCurrentPosition(sessionId: String, position: Int)
    suspend fun unlockExtraHint(sessionId: String, position: Int, questionId: String, ownerId: String)
    suspend fun finalizeSession(sessionId: String, ownerId: String): PracticeSession?
    suspend fun discardSession(sessionId: String)
    fun getCompletedSessionsForOwner(ownerId: String): Flow<List<PracticeSession>>

    // Bookmarks
    suspend fun toggleBookmark(ownerId: String, snapshot: SessionSnapshot, topicTitle: String): Boolean
    fun getBookmarksForOwner(ownerId: String): Flow<List<BookmarkItem>>
    suspend fun removeBookmark(ownerId: String, questionId: String)
}
