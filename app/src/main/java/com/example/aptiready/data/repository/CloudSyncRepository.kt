package com.example.aptiready.data.repository

import com.example.aptiready.data.model.SyncExecutionResult
import com.example.aptiready.data.model.SyncStatus
import kotlinx.coroutines.flow.StateFlow

interface CloudSyncRepository {
    val syncStatus: StateFlow<SyncStatus>
    val pendingOutboxCount: StateFlow<Int>
    val lastSyncTimestamp: StateFlow<Long>
    val isBackupEnabled: StateFlow<Boolean>

    suspend fun setBackupEnabled(enabled: Boolean)
    fun triggerSyncNow()
    suspend fun performSync(expectedOwnerId: String? = null, workerId: String? = null): SyncExecutionResult
    suspend fun restoreCloudRecords(ownerId: String): Result<Unit>
    suspend fun enqueueCompletedAttemptForSync(ownerId: String, attemptId: String, attemptType: String, payloadJson: String)
    suspend fun enqueueBookmarkForSync(ownerId: String, questionId: String, action: String, payloadJson: String)
    suspend fun retryPermanentFailures()
    suspend fun clearOutboxForOwner(ownerId: String)
}