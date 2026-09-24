package com.example.aptiready.data.remote

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.aptiready.AptiRiseApplication
import com.example.aptiready.data.model.SyncExecutionResult
import kotlinx.coroutines.CancellationException

class CloudSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as AptiRiseApplication
        val authRepo = app.appContainer.authRepository
        val cloudSyncRepo = app.appContainer.cloudSyncRepository

        val expectedOwnerId = inputData.getString("ownerId")
        val currentUid = authRepo.currentUserId
        val workerId = java.util.UUID.randomUUID().toString()

        if (!expectedOwnerId.isNullOrEmpty() && expectedOwnerId != currentUid) {
            Log.w("CloudSyncWorker", "Stopping worker: Expected owner '$expectedOwnerId' does not match active UID '$currentUid'.")
            return Result.success()
        }

        if (currentUid == null || currentUid == "guest") {
            Log.w("CloudSyncWorker", "Stopping worker: No verified active account.")
            return Result.success()
        }

        return try {
            val syncResult = cloudSyncRepo.performSync(expectedOwnerId = expectedOwnerId, workerId = workerId)
            when (syncResult) {
                SyncExecutionResult.COMPLETE -> Result.success()
                SyncExecutionResult.MORE_WORK_REMAINING -> Result.retry()
                SyncExecutionResult.TRANSIENT_FAILURE -> Result.retry()
                SyncExecutionResult.PERMANENT_FAILURE -> Result.failure()
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e("CloudSyncWorker", "Sync worker exception: ${e.message}", e)
            Result.retry()
        }
    }
}