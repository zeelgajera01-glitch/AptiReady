package com.aptirise.app.data.repository

import com.aptirise.app.data.model.MockQuestionSnapshot
import com.aptirise.app.data.model.MockTest
import com.aptirise.app.data.model.MockTestAttempt
import kotlinx.coroutines.flow.Flow

interface MockTestRepository {
    fun getMockTestsFlow(): Flow<List<MockTest>>
    suspend fun getMockTestById(testId: String): MockTest?
    suspend fun createMockAttempt(ownerId: String, testId: String): Result<MockTestAttempt>
    suspend fun getInProgressAttempt(ownerId: String): MockTestAttempt?
    fun getInProgressAttemptFlow(ownerId: String): Flow<MockTestAttempt?>
    suspend fun getAttemptById(attemptId: String): MockTestAttempt?
    fun getAttemptSnapshotsFlow(attemptId: String): Flow<List<MockQuestionSnapshot>>
    suspend fun saveOptionSelection(attemptId: String, position: Int, selectedOptionId: String?)
    suspend fun toggleMarkForReview(attemptId: String, position: Int): Boolean
    suspend fun markPositionVisited(attemptId: String, position: Int)
    suspend fun updateCurrentPosition(attemptId: String, position: Int)
    suspend fun saveTimerProgress(attemptId: String, remainingSeconds: Int, elapsedSeconds: Int)
    suspend fun finalizeAttempt(attemptId: String, finishReason: String): MockTestAttempt?
    suspend fun discardAttempt(attemptId: String)
    fun getCompletedAttemptsForOwner(ownerId: String): Flow<List<MockTestAttempt>>
    suspend fun getLastCompletedAttemptForTest(ownerId: String, testId: String): MockTestAttempt?
}
