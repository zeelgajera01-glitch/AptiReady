package com.example.aptiready.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncOutboxDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOutboxItem(item: SyncOutboxEntity)

    @Query("""
        UPDATE sync_outbox 
        SET status = 'SYNCING', workerId = :workerId, claimedAt = :now, lastAttemptedAt = :now
        WHERE id IN (
            SELECT id FROM sync_outbox 
            WHERE ownerId = :ownerId 
              AND (status IN ('PENDING', 'FAILED') OR (status = 'SYNCING' AND (claimedAt IS NULL OR claimedAt < :staleThresholdMs)))
            ORDER BY createdAt ASC
            LIMIT 50
        )
    """)
    suspend fun claimPendingItemsForWorker(ownerId: String, workerId: String, now: Long, staleThresholdMs: Long): Int

    @Query("SELECT * FROM sync_outbox WHERE ownerId = :ownerId AND workerId = :workerId AND status = 'SYNCING' ORDER BY createdAt ASC")
    suspend fun getClaimedItemsForWorker(ownerId: String, workerId: String): List<SyncOutboxEntity>

    @Query("SELECT * FROM sync_outbox WHERE ownerId = :ownerId ORDER BY createdAt ASC")
    suspend fun getAllOutboxItemsForOwner(ownerId: String): List<SyncOutboxEntity>

    @Query("""
        SELECT COUNT(*) FROM sync_outbox 
        WHERE ownerId = :ownerId 
          AND status IN ('PENDING', 'FAILED', 'SYNCING')
    """)
    fun getPendingCountFlow(ownerId: String): Flow<Int>

    @Query("""
        SELECT COUNT(*) FROM sync_outbox
        WHERE ownerId = :ownerId
          AND status IN ('FAILED_PERMANENT', 'BLOCKED_CONFLICT')
    """)
    fun getPermanentFailureCountFlow(ownerId: String): Flow<Int>

    @Query("""
        SELECT COUNT(*) FROM sync_outbox
        WHERE ownerId = :ownerId
          AND status IN ('FAILED_PERMANENT', 'BLOCKED_CONFLICT')
    """)
    suspend fun getPermanentFailureCount(ownerId: String): Int

    @Query("""
        UPDATE sync_outbox
        SET status = 'PENDING',
            retryCount = 0,
            workerId = NULL,
            claimedAt = NULL
        WHERE ownerId = :ownerId
          AND status = 'FAILED_PERMANENT'
    """)
    suspend fun resetPermanentFailuresForOwner(ownerId: String): Int

    @Query("SELECT * FROM sync_outbox WHERE id = :id LIMIT 1")
    suspend fun getOutboxItemById(id: String): SyncOutboxEntity?

    @Query("SELECT * FROM sync_outbox WHERE ownerId = :ownerId AND recordType = :recordType AND recordId = :recordId LIMIT 1")
    suspend fun getOutboxItemByRecord(ownerId: String, recordType: String, recordId: String): SyncOutboxEntity?

    @Query("UPDATE sync_outbox SET status = :status, lastAttemptedAt = :lastAttemptedAt, retryCount = retryCount + 1 WHERE id = :id AND ownerId = :ownerId AND workerId = :workerId AND revision = :revision")
    suspend fun updateItemStatusWithRevision(id: String, ownerId: String, workerId: String, revision: Int, status: String, lastAttemptedAt: Long): Int

    @Query("UPDATE sync_outbox SET status = :status, lastAttemptedAt = :lastAttemptedAt, retryCount = retryCount + 1 WHERE id = :id")
    suspend fun updateItemStatus(id: String, status: String, lastAttemptedAt: Long)

    @Query("DELETE FROM sync_outbox WHERE id = :id AND ownerId = :ownerId AND workerId = :workerId AND revision = :revision")
    suspend fun deleteClaimedOutboxItemWithRevision(id: String, ownerId: String, workerId: String, revision: Int): Int

    @Query("DELETE FROM sync_outbox WHERE id = :id AND ownerId = :ownerId AND workerId = :workerId")
    suspend fun deleteClaimedOutboxItem(id: String, ownerId: String, workerId: String): Int

    @Query("DELETE FROM sync_outbox WHERE id = :id")
    suspend fun deleteOutboxItem(id: String)

    @Query("DELETE FROM sync_outbox WHERE ownerId = :ownerId")
    suspend fun deleteOutboxForOwner(ownerId: String)
}