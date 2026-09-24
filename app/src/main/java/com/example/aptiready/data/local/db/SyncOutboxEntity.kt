package com.example.aptiready.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_outbox")
data class SyncOutboxEntity(
    @PrimaryKey val id: String, // "${ownerId}_${recordType}_${recordId}"
    val ownerId: String,
    val recordId: String,
    val recordType: String, // "PRACTICE", "MOCK_TEST", "BOOKMARK"
    val action: String, // "UPSERT", "DELETE"
    val payloadJson: String,
    val status: String, // "PENDING", "SYNCING", "FAILED", "FAILED_PERMANENT"
    val retryCount: Int,
    val createdAt: Long,
    val lastAttemptedAt: Long?,
    val workerId: String? = null,
    val claimedAt: Long? = null,
    val revision: Int = 1
)