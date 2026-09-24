package com.example.aptiready.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "synced_attempts")
data class SyncedAttemptEntity(
    @PrimaryKey val attemptId: String,
    val ownerId: String,
    val syncedAt: Long
)