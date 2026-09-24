package com.example.aptiready.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SyncedAttemptDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncedAttempt(item: SyncedAttemptEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM synced_attempts WHERE attemptId = :attemptId)")
    suspend fun isSynced(attemptId: String): Boolean

    @Query("DELETE FROM synced_attempts WHERE ownerId = :ownerId")
    suspend fun clearSyncedAttemptsForOwner(ownerId: String)
}