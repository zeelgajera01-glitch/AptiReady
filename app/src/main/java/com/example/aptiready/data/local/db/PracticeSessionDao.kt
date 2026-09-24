package com.example.aptiready.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PracticeSessionDao {
    @Query("""
        UPDATE session_question_snapshots
        SET isExtraHintUnlocked = 1
        WHERE sessionId = :sessionId
          AND position = :position
          AND questionId = :questionId
          AND (
              TRIM(hint) != ''
              OR TRIM(extraHint) != ''
          )
          AND isExtraHintUnlocked = 0
          AND EXISTS (
              SELECT 1
              FROM practice_sessions s
              WHERE s.id = :sessionId
                AND s.ownerId = :ownerId
          )
    """)
    suspend fun unlockExtraHintOwned(
        sessionId: String,
        position: Int,
        questionId: String,
        ownerId: String
    ): Int


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: PracticeSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshots(snapshots: List<SessionQuestionSnapshotEntity>)

    @Update
    suspend fun updateSession(session: PracticeSessionEntity)

    @Update
    suspend fun updateSnapshot(snapshot: SessionQuestionSnapshotEntity)

    @Query("SELECT * FROM practice_sessions WHERE ownerId = :ownerId AND status = 'IN_PROGRESS' ORDER BY createdAt DESC LIMIT 1")
    fun getInProgressSessionFlow(ownerId: String): Flow<PracticeSessionEntity?>

    @Query("SELECT * FROM practice_sessions WHERE ownerId = :ownerId AND status = 'IN_PROGRESS' ORDER BY createdAt DESC LIMIT 1")
    suspend fun getInProgressSession(ownerId: String): PracticeSessionEntity?

    @Query("SELECT * FROM practice_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: String): PracticeSessionEntity?

    @Query("SELECT * FROM session_question_snapshots WHERE sessionId = :sessionId ORDER BY position ASC")
    suspend fun getSnapshotsForSession(sessionId: String): List<SessionQuestionSnapshotEntity>

    @Query("""
        DELETE FROM session_question_snapshots
        WHERE sessionId = :sessionId
    """)
    suspend fun deleteSnapshotsForSession(sessionId: String): Int

    @Query("SELECT * FROM session_question_snapshots WHERE sessionId = :sessionId ORDER BY position ASC")
    fun getSnapshotsForSessionFlow(sessionId: String): Flow<List<SessionQuestionSnapshotEntity>>

    @Query("SELECT * FROM practice_sessions WHERE ownerId = :ownerId AND status = 'COMPLETED' ORDER BY completedAt DESC")
    fun getCompletedSessionsForOwner(ownerId: String): Flow<List<PracticeSessionEntity>>

    @Query("SELECT * FROM practice_sessions WHERE ownerId = :ownerId AND status = 'COMPLETED' ORDER BY completedAt DESC")
    suspend fun getCompletedSessionsListForOwner(ownerId: String): List<PracticeSessionEntity>

    @Query("""
        SELECT DISTINCT sqs.questionId 
        FROM session_question_snapshots sqs
        INNER JOIN practice_sessions ps ON sqs.sessionId = ps.id
        WHERE ps.ownerId = :ownerId 
          AND ps.topicId = :topicId 
          AND ps.status = 'COMPLETED'
          AND sqs.isSubmitted = 1
    """)
    suspend fun getSeenQuestionIdsForTopic(ownerId: String, topicId: String): List<String>

    @Query("""
        SELECT DISTINCT sqs.questionId 
        FROM session_question_snapshots sqs
        INNER JOIN practice_sessions ps ON sqs.sessionId = ps.id
        WHERE ps.ownerId = :ownerId 
          AND ps.topicId = :topicId 
          AND ps.status = 'IN_PROGRESS'
    """)
    suspend fun getActiveReservedQuestionIdsForTopic(ownerId: String, topicId: String): List<String>

    @Query("""
        SELECT snapshot.*
        FROM session_question_snapshots AS snapshot
        INNER JOIN practice_sessions AS session
            ON snapshot.sessionId = session.id
        WHERE session.ownerId = :ownerId
          AND session.status = 'COMPLETED'
        ORDER BY snapshot.sessionId, snapshot.position
    """)
    fun observeCompletedSnapshotsForOwner(
        ownerId: String
    ): Flow<List<SessionQuestionSnapshotEntity>>
}
