package com.example.aptiready.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MockTestDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMockTests(tests: List<MockTestEntity>)

    @Query("SELECT * FROM mock_tests WHERE published = 1 ORDER BY id ASC")
    fun getMockTestsFlow(): Flow<List<MockTestEntity>>

    @Query("SELECT * FROM mock_tests WHERE id = :testId LIMIT 1")
    suspend fun getMockTestById(testId: String): MockTestEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttempt(attempt: MockTestAttemptEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshots(snapshots: List<MockQuestionSnapshotEntity>)

    @Update
    suspend fun updateAttempt(attempt: MockTestAttemptEntity)

    @Update
    suspend fun updateSnapshot(snapshot: MockQuestionSnapshotEntity)

    @Query("SELECT * FROM mock_test_attempts WHERE ownerId = :ownerId AND status = 'IN_PROGRESS' ORDER BY createdAt DESC LIMIT 1")
    fun getInProgressAttemptFlow(ownerId: String): Flow<MockTestAttemptEntity?>

    @Query("SELECT * FROM mock_test_attempts WHERE ownerId = :ownerId AND status = 'IN_PROGRESS' ORDER BY createdAt DESC LIMIT 1")
    suspend fun getInProgressAttempt(ownerId: String): MockTestAttemptEntity?

    @Query("SELECT * FROM mock_test_attempts WHERE id = :attemptId LIMIT 1")
    suspend fun getAttemptById(attemptId: String): MockTestAttemptEntity?

    @Query("SELECT * FROM mock_question_snapshots WHERE attemptId = :attemptId ORDER BY position ASC")
    suspend fun getSnapshotsForAttempt(attemptId: String): List<MockQuestionSnapshotEntity>

    @Query("""
        DELETE FROM mock_question_snapshots
        WHERE attemptId = :attemptId
    """)
    suspend fun deleteSnapshotsForAttempt(attemptId: String): Int

    @Query("SELECT * FROM mock_question_snapshots WHERE attemptId = :attemptId ORDER BY position ASC")
    fun getSnapshotsForAttemptFlow(attemptId: String): Flow<List<MockQuestionSnapshotEntity>>

    @Query("SELECT * FROM mock_test_attempts WHERE ownerId = :ownerId AND status = 'COMPLETED' ORDER BY completedAt DESC")
    fun getCompletedAttemptsForOwner(ownerId: String): Flow<List<MockTestAttemptEntity>>

    @Query("SELECT * FROM mock_test_attempts WHERE ownerId = :ownerId AND status = 'COMPLETED' ORDER BY completedAt DESC")
    suspend fun getCompletedAttemptsListForOwner(ownerId: String): List<MockTestAttemptEntity>

    @Query("SELECT * FROM mock_test_attempts WHERE ownerId = :ownerId AND testId = :testId AND status = 'COMPLETED' ORDER BY completedAt DESC LIMIT 1")
    suspend fun getLastCompletedAttemptForTest(ownerId: String, testId: String): MockTestAttemptEntity?

    @Query("""
        SELECT snapshot.*
        FROM mock_question_snapshots AS snapshot
        INNER JOIN mock_test_attempts AS attempt
            ON snapshot.attemptId = attempt.id
        WHERE attempt.ownerId = :ownerId
          AND attempt.status = 'COMPLETED'
        ORDER BY snapshot.attemptId, snapshot.position
    """)
    fun observeCompletedSnapshotsForOwner(
        ownerId: String
    ): Flow<List<MockQuestionSnapshotEntity>>
}